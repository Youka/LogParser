package io.youka.logparser

import com.github.ajalt.clikt.core.PrintHelpMessage
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InterfaceTest {
    @Test
    fun testCLIHelp() {
        assertFailsWith(PrintHelpMessage::class) {
            // Caution: CLI main method causes System.exit call
            CLI().parse(arrayOf("-h"))
        }
    }

    @Test
    fun testCLIPerformance() =
        assertTrue(
            measureTimeMillis {
                CLI().parse(arrayOf("./test/logs", "-o", "./test/logparser_results", "-po"))
            } <= 1000,
            "CLI run took >1 second!"
        )
}