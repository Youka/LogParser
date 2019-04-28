package io.youka.logparser

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import io.youka.logparser.processing.LogParser
import io.youka.logparser.utils.ProjectProperties
import io.youka.logparser.utils.option
import java.io.File

/**
 * Command line interface definition.
 *
 * @author Christoph Spanknebel
 */
class CLI : CliktCommand(name = ProjectProperties.name, help = ProjectProperties.description, epilog = "<${ProjectProperties.year}, ${ProjectProperties.organization}>") {
     // Version & environment variables.
    init {
        this.versionOption(ProjectProperties.version, names = setOf("-v", "--version"))
        // See symbol <com.github.ajalt.clikt.parameters.options.inferEnvvar>
        this.context { this.autoEnvvarPrefix = commandName.toUpperCase() }
    }

    // I/O
    private val input: File by argument("input", "File or directory to traverse for log parsing")
                                .file(exists = true, readable = true)
    private val outputDir: File by option("-o", "--output", help = "Directory to write results of parsing", default = LogParser.DEFAULT_OUTPUT_DIR)
                                    .file(fileOkay = false)
                                    .default(File(LogParser.DEFAULT_OUTPUT_DIR))

    // Search pattern
    private val filePattern: List<String> by option("-fp", "--filePattern", help = "Pattern to match filenames with wildcards", default = LogParser.DEFAULT_FILE_PATTERN)
                                            .multiple(listOf(LogParser.DEFAULT_FILE_PATTERN))
    private val linePattern: List<String> by option("-lp", "--linePattern", help = "Regex pattern to detect lines of interest", default = LogParser.DEFAULT_LINE_PATTERN)
                                            .multiple(listOf(LogParser.DEFAULT_LINE_PATTERN))
    private val timePattern: String by option("-tp", "--timePattern", help = "Regex pattern to find timestamp in lines of interest", default = LogParser.DEFAULT_TIME_PATTERN)
                                        .default(LogParser.DEFAULT_TIME_PATTERN)
    private val continuousLinePattern: List<String> by option("-clp", "--continuousLinePattern", help = "Regex pattern to detect lines after line of interest", default = LogParser.DEFAULT_CONTINUOUS_LINE_PATTERN)
                                                        .multiple(listOf(LogParser.DEFAULT_CONTINUOUS_LINE_PATTERN))

    // Output filter
    private val continuousLineCount: Int by option("-clc", "--continuousLineCount", help = "Maximal number of lines after line of interest", default = LogParser.DEFAULT_CONTINUOUS_LINE_COUNT)
                                            .int()
                                            .restrictTo(min = 0, clamp = true)
                                            .default(LogParser.DEFAULT_CONTINUOUS_LINE_COUNT)
    private val prependOrigin: Boolean by option("-po", "--prependOrigin", help = "Prepend origin information to line of interest", default = LogParser.DEFAULT_PREPEND_ORIGIN)
                                            .flag("-pooff", default = LogParser.DEFAULT_PREPEND_ORIGIN)
    private val aggregate: Boolean by option("-ag", "--aggregate", help = "Aggregate lines of interest", default = LogParser.DEFAULT_AGGREGATE)
                                        .flag("-agoff", default = LogParser.DEFAULT_AGGREGATE)

    /**
     * On successful command line input, run processing.
     *
     * @throws CliktError Error type recognized and correctly handled by command line processor (Clikt)
     */
    override fun run() =
        try {
            LogParser(
                this.input,
                this.outputDir,
                this.filePattern,
                this.linePattern,
                this.timePattern,
                this.continuousLinePattern,
                this.continuousLineCount,
                this.prependOrigin,
                this.aggregate
            )
                .parse()
        } catch(e: Exception) {
            // Pack in error type recognized by Clikt main method for bad exit
            throw CliktError("Error occurred while running: ${e.message}", e)
        }
}