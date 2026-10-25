package app.vyrlume.mobile.demo

import app.vyrlume.mobile.data.DemoWorkspace
import app.vyrlume.mobile.data.TerminalLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoTerminalTest {
    @Test
    fun `lists actual demo files`() {
        val lines = DemoTerminal.execute("ls", DemoWorkspace.files)
        DemoWorkspace.files.forEach { file ->
            assertTrue(lines.any { it.text == file.path })
        }
    }

    @Test
    fun `reports unknown command as error`() {
        val lines = DemoTerminal.execute("rm -rf /", DemoWorkspace.files)
        assertTrue(lines.last().tone == TerminalLine.Tone.ERROR)
        assertTrue(lines.last().text.contains("command not found"))
    }

    @Test
    fun `marks test output explicitly simulated`() {
        val lines = DemoTerminal.execute("npm test", DemoWorkspace.files)
        assertTrue(lines.any { it.text.contains("simulated demo result") })
    }
}
