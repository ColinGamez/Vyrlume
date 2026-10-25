package app.vyrlume.mobile.data

import java.util.UUID

enum class WorkspaceKind { DEMO, REMOTE }

enum class IdeSection(val label: String) {
    EDITOR("Editor"),
    FILES("Files"),
    TERMINAL("Terminal"),
    SOURCE_CONTROL("Git"),
    PREVIEW("Preview"),
}

data class ServerConnection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val lastUsed: Long = System.currentTimeMillis(),
)

data class WorkspaceFile(
    val path: String,
    val language: String,
    val content: String,
    val dirty: Boolean = false,
)

data class TerminalLine(
    val text: String,
    val tone: Tone = Tone.OUTPUT,
) {
    enum class Tone { INPUT, OUTPUT, SUCCESS, ERROR, MUTED }
}

object DemoWorkspace {
    val files = listOf(
        WorkspaceFile(
            path = "src/main/kotlin/Main.kt",
            language = "Kotlin",
            content = """
                package app.vyrlume.demo

                data class Build(val name: String, val healthy: Boolean)

                fun main() {
                    val build = Build("Vyrlume", healthy = true)
                    println("Launching ${'$'}{build.name}…")
                    check(build.healthy) { "Build needs attention" }
                    println("Ready on http://localhost:3000")
                }
            """.trimIndent(),
        ),
        WorkspaceFile(
            path = "web/app.ts",
            language = "TypeScript",
            content = """
                type Project = { name: string; files: number };
                const project: Project = { name: "Aero Notes", files: 4 };

                export function greet(name: string): string {
                    return `Hello, ${'$'}{name}!`;
                }

                console.log(greet(project.name));
            """.trimIndent(),
        ),
        WorkspaceFile(
            path = "web/styles.css",
            language = "CSS",
            content = """
                :root {
                  color-scheme: dark;
                  --sky: #58d8ff;
                  --mint: #a5e65a;
                  --ink: #071d2d;
                }

                body {
                  margin: 0;
                  min-height: 100vh;
                  font-family: "Segoe UI", sans-serif;
                  background: linear-gradient(160deg, #0b6f9d, var(--ink));
                }
            """.trimIndent(),
        ),
        WorkspaceFile(
            path = "README.md",
            language = "Markdown",
            content = """
                # Aero Notes

                A small project included with **Vyrlume** so you can explore the mobile workspace without a server.

                ## Try it

                1. Edit a file and tap Save.
                2. Open Terminal and run `npm test`.
                3. Open Preview to interact with the demo web app.

                > This workspace is an interactive local demo, not a remote Linux environment.
            """.trimIndent(),
        ),
    )
}
