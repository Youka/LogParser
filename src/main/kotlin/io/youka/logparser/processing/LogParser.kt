package io.youka.logparser.processing

import io.youka.logparser.utils.*
import java.io.File
import java.io.IOException

/**
 * LogParser main class. Does file processing by parameters.
 *
 * @property input File or directory to traverse for log parsing
 * @property outputDir Directory to write results of parsing
 * @property filePattern Pattern to match filenames with wildcards
 * @property linePattern Regex pattern to detect lines of interest
 * @property continuousLinePattern Regex pattern to detect lines after line of interest
 * @property timePattern Regex pattern to find timestamps in lines of interest
 * @property continuousLineCount Maximal number of lines after line of interest
 * @property prependOrigin Prepend origin information to line of interest
 * @property aggregate Aggregate lines of interest
 * @author Christoph Spanknebel
 */
data class LogParser(
    var input: File, var outputDir: File = File(DEFAULT_OUTPUT_DIR),
    var filePattern: List<String> = listOf(DEFAULT_FILE_PATTERN), var linePattern: List<String> = listOf(
        DEFAULT_LINE_PATTERN
    ),
    var timePattern: String = DEFAULT_TIME_PATTERN, var continuousLinePattern: List<String> = listOf(
        DEFAULT_CONTINUOUS_LINE_PATTERN
    ),
    var continuousLineCount: Int = DEFAULT_CONTINUOUS_LINE_COUNT, var prependOrigin: Boolean = DEFAULT_PREPEND_ORIGIN, var aggregate: Boolean = DEFAULT_AGGREGATE
) : Logger() {
    // Recommended default values.
    companion object {
        /** Default value for [outputDir] */
        const val DEFAULT_OUTPUT_DIR = "./logparser_results"
        /** Default value for [filePattern] */
        const val DEFAULT_FILE_PATTERN = "*.log"
        /** Default value for [linePattern] */
        const val DEFAULT_LINE_PATTERN = ".*(E|e)xception:.+"
        /** Default value for [timePattern] */
        const val DEFAULT_TIME_PATTERN = "\\[[\\d\\.:aApPmM ]+\\]\\s?"
        /** Default value for [continuousLinePattern] */
        const val DEFAULT_CONTINUOUS_LINE_PATTERN = "(\\s+at|Caused by:).+"
        /** Default value for [continuousLineCount] */
        const val DEFAULT_CONTINUOUS_LINE_COUNT = 50
        /** Default value for [prependOrigin] */
        const val DEFAULT_PREPEND_ORIGIN = false
        /** Default value for [aggregate] */
        const val DEFAULT_AGGREGATE = false
    }

    /**
     * Run parser to process files.
     *
     * @throws Exception Any type of exception may be thrown from internal
     */
    fun parse() {
        // Check input
        if(!this.input.exists())
            throw IOException("Input file or directory doesn't exist: ${this.input.path}")
        // Make clean output directory
        if(!this.outputDir.createDirectory())
            throw IOException("Couldn't make new output directory: ${this.outputDir.path}")
        // Continue with direct parameters
        this.parse(
            this.filePattern.map { pattern -> pattern.toWildcardRegex() },
            this.linePattern.map { pattern -> pattern.toRegex() },
            this.timePattern.toRegex(),
            this.continuousLinePattern.map { pattern -> pattern.toRegex() },
            if(this.continuousLineCount < 0) Int.MAX_VALUE else this.continuousLineCount
        )
    }

    // Processing after preparation
    private fun parse(filePatternCompiled: List<Regex>, linePatternCompiled: List<Regex>, timePatternCompiled: Regex, continuousLinePatternCompiled: List<Regex>, continuousLineCountPositive: Int) {
        // With or w/o aggregation?
        if(this.aggregate) {
            val logAggregation = LogAggregation()
            this.input.walkFiles(filePatternCompiled) { file ->
                this.aggregateFile(logAggregation, file, linePatternCompiled, timePatternCompiled, continuousLinePatternCompiled, continuousLineCountPositive)
            }
            this.writeAggregationFiles(logAggregation)
        } else
            this.input.walkFiles(filePatternCompiled) { file ->
                this.writeFilteredFile(file, linePatternCompiled, continuousLinePatternCompiled, continuousLineCountPositive)
            }
    }

    // Aggregation
    private fun aggregateFile(aggregation: LogAggregation, file: File, linePatternCompiled: List<Regex>, timePatternCompiled: Regex, continuousLinePatternCompiled: List<Regex>, continuousLineCountPositive: Int) {
        // Log buffer
        lateinit var originState: LogOrigin
        val entryBuffer = StringBuilder()
        val addEntry = {
            entryBuffer.takeIf { it.isNotEmpty() }?.apply {
                aggregation[this.toStringAndClear()] = originState
            }
        }
        // Process lines
        forEachLineOfInterest(file, linePatternCompiled, continuousLinePatternCompiled, continuousLineCountPositive) { line, index, continuous ->
            if(continuous) {
                entryBuffer.appendln(line)
            } else {
                addEntry()
                originState = LogOrigin(
                    index,
                    timePatternCompiled.find(line)?.value?.trim() ?: "",
                    file
                )
                entryBuffer.appendln(timePatternCompiled.replaceFirst(line, ""))
            }
        }
        addEntry()
    }
    private fun writeAggregationFiles(aggregation: LogAggregation) {
        var fileIndex = 0
        for((entry, origin) in aggregation)
            // Create output file and start writing
            File(this.outputDir, "entry${fileIndex++}.log").also { it.createNewFile() }.writer().buffered().use { writer ->
                // Write text & origins
                writer.appendln("### Text ###")
                    .appendln(entry)
                    .appendln("### Origins (${origin.size}) ###")
                    .apply {
                        origin.forEach {
                            this.appendln("Time: ${it.timestamp} - File: ${it.file.absolutePath} - Line: ${it.lineNumber}")
                        }
                    }
            }
    }

    // Simple output (w/o aggregation)
    private fun writeFilteredFile(file: File, linePatternCompiled: List<Regex>, continuousLinePatternCompiled: List<Regex>, continuousLineCountPositive: Int) {
        // Prepare output file
        val relativePath = file.toRelativeString(this.input).ifEmpty { file.name }
        this.logger.info("Found: $relativePath")
        val outputFile = File(this.outputDir, relativePath)
        // Writer to output file
        val outputWriter by lazy {
            // Create output file
            if(!outputFile.createNewFileWithDirs())
                throw IOException("Couldn't make new file: ${outputFile.absolutePath}")
            // Create writer to output file
            outputFile.printWriter().buffered()
        }
        try {
            // Process lines
            forEachLineOfInterest(file, linePatternCompiled, continuousLinePatternCompiled, continuousLineCountPositive) { line, index, continuous ->
                if(continuous)
                    outputWriter.appendln(line)
                else {
                    outputWriter.takeIf { this.prependOrigin }?.appendln("### Original line: $index ###")
                    outputWriter.appendln(line)
                }
            }
        } finally {
            // Clean writer
            if (outputFile.exists())
                outputWriter.close()
        }
    }

    // Helpers
    private inline fun forEachLineOfInterest(file: File, linePattern: Collection<Regex>, continuousLinePattern: Collection<Regex>, continuousLineCountPositive: Int, crossinline action: (line: String, index: Long, continuous: Boolean) -> Unit) {
        var continuousCount: Int? = null
        file.forEachLineIndexed { line, index ->
            // Detect & handle line of interest
            continuousCount = when {
                // Found line after line of interest
                continuousCount != null && continuousLinePattern.any { regex -> regex.matches(line) } -> {
                    val continuousCountSafe = continuousCount ?: Int.MAX_VALUE
                    // Limit reached?
                    if(continuousCountSafe < continuousLineCountPositive) {
                        action(line, index, true)
                        continuousCountSafe + 1
                    } else
                        continuousCountSafe
                }
                // Found line of interest
                linePattern.any { regex -> regex.matches(line) } -> {
                    action(line, index, false)
                    0
                }
                // Found nothing
                else -> null
            }
        }
    }
}