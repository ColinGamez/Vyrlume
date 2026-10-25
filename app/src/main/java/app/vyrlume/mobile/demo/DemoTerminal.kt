package app.vyrlume.mobile.demo

import app.vyrlume.mobile.data.TerminalLine
import app.vyrlume.mobile.data.TerminalLine.Tone
import app.vyrlume.mobile.data.WorkspaceFile

object DemoTerminal {
    val welcome = listOf(
        TerminalLine("Vyrlume interactive demo shell", Tone.SUCCESS),
        TerminalLine("Commands run against the bundled sample project only. Type ‘help’.", Tone.MUTED),
    )

    fun execute(command: String, files: List<WorkspaceFile>): List<TerminalLine> {
        val normalized = command.trim()
        if (normalized.isEmpty()) return emptyList()
        val output = when (normalized.lowercase()) {
            "help" -> listOf(
                "Available demo commands:",
                "  ls             list sample project files",
                "  git status     show unsaved sample changes",
                "  npm test       run the simulated sample checks",
                "  npm run dev    start the simulated preview",
                "  pwd            print the demo workspace path",
                "  whoami         identify this demo session",
                "  clear          clear this terminal",
            )
            "ls", "ls -la" -> files.map { it.path }
            "git status" -> buildList {
                add("On branch main — demo repository")
                val dirty = files.filter { it.dirty }
                if (dirty.isEmpty()) add("nothing to commit, working tree clean")
                else {
                    add("Changes not staged for commit:")
                    dirty.forEach { add("  modified: ${it.path}") }
                }
            }
            "npm test" -> listOf(
                "> aero-notes@1.0.0 test",
                "PASS demo editor renders sample files",
                "PASS preview interaction updates counter",
                "Tests: 2 passed, 2 total (simulated demo result)",
            )
            "npm run dev" -> listOf(
                "> aero-notes@1.0.0 dev",
                "Demo preview ready on port 3000",
                "Open Preview to interact with it.",
            )
            "pwd" -> listOf("/home/aero/demo-workspace")
            "whoami" -> listOf("aero-demo")
            else -> listOf("demo: command not found: $normalized")
        }
        val isError = normalized.lowercase() !in setOf(
            "help", "ls", "ls -la", "git status", "npm test", "npm run dev", "pwd", "whoami", "clear",
        )
        return buildList {
            add(TerminalLine("aero@demo:~/workspace$ $command", Tone.INPUT))
            output.forEach { line ->
                add(
                    TerminalLine(
                        text = line,
                        tone = when {
                            isError -> Tone.ERROR
                            line.startsWith("PASS") || line.contains("ready", true) -> Tone.SUCCESS
                            else -> Tone.OUTPUT
                        },
                    ),
                )
            }
        }
    }
}
