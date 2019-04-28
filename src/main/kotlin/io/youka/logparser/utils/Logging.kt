package io.youka.logparser.utils

import java.util.logging.LogManager
import java.util.logging.Logger

/**
 * Adds logger getter to implementing class.
 *
 * @author Christoph Spanknebel
 */
interface Loggable {
    /**
     * Updates logging system by configuration file (resource **logging.properties**) and returns logger for this class.
     *
     * @return New logger instance
     */
    fun logger() : Logger {
        this.javaClass.getResourceAsStream("/logging.properties")?.use { stream -> LogManager.getLogManager()?.readConfiguration(stream) }
        return Logger.getLogger(this.javaClass.name)!!
    }
}

/**
 * Implementation with logger property.
 *
 * @property logger Lazy initialized logger instance
 * @see Loggable
 * @author Christoph Spanknebel
 */
open class Logger : Loggable {
    protected val logger by lazy { this.logger() }
}