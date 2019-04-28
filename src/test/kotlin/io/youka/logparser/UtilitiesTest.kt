package io.youka.logparser

import com.github.ajalt.clikt.core.NoRunCliktCommand
import io.youka.logparser.utils.Loggable
import io.youka.logparser.utils.ProjectProperties
import io.youka.logparser.utils.option
import io.youka.logparser.utils.toWildcardRegex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtilitiesTest: Loggable {
    @Test
    fun testProjectProperties() =
        assertEquals("LogParser", ProjectProperties.name)

    @Test
    fun testWildcardRegex(): Unit =
        mapOf("*.log" to ("test.log" to "test.txt"), "log*.txt" to ("log_01011970.txt" to "access_01011970.txt")).forEach { pattern, textPair ->
            val (trueText, falseText) = textPair
            assertTrue(pattern.toWildcardRegex().matches(trueText), "Pattern '$pattern' didn't match to '$trueText'!")
            assertFalse(pattern.toWildcardRegex().matches(falseText), "Pattern '$pattern' shouldn't match to '$falseText'!")
        }

    @Test
    fun testLogging() =
        this.logger().info("Logging went fine.")

    @Test
    fun testOptionWithDefault() {
        val optionHelp = "Find your own answer"
        val optionDefault = 42
        val cmdHelp = NoRunCliktCommand().also {
            it.registerOption(it.option("-test", help = optionHelp, default = optionDefault))
        }.getFormattedHelp()
        assertTrue(
            cmdHelp.contains(Regex("\\s+$optionHelp[^\n]*default[^\n]*$optionDefault")),
            "Command help didn't contain option default: $optionDefault\n$cmdHelp"
        )
    }
}