package io.youka.logparser.processing

import java.io.File

/**
 * Storage of unique log entries emerging multiple times in files.
 *
 * @author Christoph Spanknebel
 */
class LogAggregation : HashMap<LogEntry, ArrayList<LogOrigin>>() {
    /**
     * Ensures origins list to entry exists before adding value.
     *
     * @param entry Unique entry
     * @param origin Origin to add to entry list
     */
    operator fun set(entry: LogEntry, origin: LogOrigin) {
        this.getOrPut(entry) { arrayListOf() }.add(origin)
    }
}

/**
 * Log entries are a block of text
 */
typealias LogEntry = String
/**
 * Log entries start from a line in a file
 */
data class LogOrigin(val lineNumber: Long, val timestamp: String, val file: File)