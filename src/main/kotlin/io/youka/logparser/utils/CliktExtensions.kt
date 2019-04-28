package io.youka.logparser.utils

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.option

/**
 * Wrapper around option with values. Adds [default] parameter.
 *
 * @param default Default value appendix to help text
 */
// See symbol <com.github.ajalt.clikt.parameters.options.option>
fun CliktCommand.option(vararg names: String, help: String = "", metavar: String? = null,
                        hidden: Boolean = false, envvar: String? = null, default: Any?): RawOption =
    this.option(*names, help = if(default != null) "$help <default: $default>" else help, metavar = metavar, hidden = hidden, envvar = envvar)