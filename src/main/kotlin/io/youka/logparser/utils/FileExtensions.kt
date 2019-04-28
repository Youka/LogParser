package io.youka.logparser.utils

import java.io.File

/**
 * Creates new directory after deleting old path keeper.
 *
 * @return Could delete old file/directory (if existed) and created new directory
 */
fun File.createDirectory() = (!this.exists() || this.deleteRecursively()) && this.mkdirs()

/**
 * Create new file and intermediate directories.
 *
 * @return Created new file
 */
fun File.createNewFileWithDirs() = this.also { it.parentFile?.mkdirs() }.createNewFile()

/**
 * Walks files in directory top-down and optionally filters by provided whitelist.
 *
 * @param whitelist Collection of regular expressions, at least one entry has to match to pass file
 * @param block Block gets called for every passed file
 */
inline fun File.walkFiles(whitelist: Collection<Regex>? = null, crossinline block: (file: File) -> Unit) {
    for(file: File in this.walkTopDown())
        if(file.isFile && whitelist?.any { pattern -> pattern.matches(file.name) }.let { any ->  if(any == true) null else false } === null)
            block(file)
}

/**
 * Iterates file lines with index.
 *
 * @param action Block gets called for each line with index
 */
inline fun File.forEachLineIndexed(crossinline action: (line: String, index: Long) -> Unit) {
    var lineIndex = 0L
    this.forEachLine { line ->
        action(line, ++lineIndex)
    }
}