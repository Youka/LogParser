package io.youka.logparser.utils

/**
 * Converter for regex to match simple wildcard pattern.
 *
 * @return Regex to match wildcards only
 */
fun String.toWildcardRegex() = "\\Q$this\\E".replace("*", "\\E.*\\Q").toRegex()

/**
 * Returns containing string and cleans builder afterwards.
 *
 * @return Builder content
 */
fun StringBuilder.toStringAndClear() = this.let { builder -> val str = builder.toString(); builder.clear(); str }