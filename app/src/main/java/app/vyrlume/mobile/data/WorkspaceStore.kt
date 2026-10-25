package app.vyrlume.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class WorkspaceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "vyrlume_workspace",
        Context.MODE_PRIVATE,
    )

    fun loadServers(): List<ServerConnection> {
        val raw = preferences.getString(KEY_SERVERS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        ServerConnection(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            url = item.getString("url"),
                            lastUsed = item.optLong("lastUsed"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList()).sortedByDescending { it.lastUsed }
    }

    fun saveServers(servers: List<ServerConnection>) {
        val array = JSONArray()
        servers.forEach { server ->
            array.put(
                JSONObject()
                    .put("id", server.id)
                    .put("name", server.name)
                    .put("url", server.url)
                    .put("lastUsed", server.lastUsed),
            )
        }
        preferences.edit().putString(KEY_SERVERS, array.toString()).apply()
    }

    fun loadActiveServerId(): String? = preferences.getString(KEY_ACTIVE_SERVER, null)

    fun saveActiveServerId(id: String?) {
        preferences.edit().apply {
            if (id == null) remove(KEY_ACTIVE_SERVER) else putString(KEY_ACTIVE_SERVER, id)
        }.apply()
    }

    fun hasEnteredWorkspace(): Boolean = preferences.getBoolean(KEY_ENTERED, false)

    fun setEnteredWorkspace(entered: Boolean) {
        preferences.edit().putBoolean(KEY_ENTERED, entered).apply()
    }

    fun loadDemoFiles(): List<WorkspaceFile> {
        val defaults = DemoWorkspace.files.map { file ->
            file.copy(content = preferences.getString(fileKey(file.path), null) ?: file.content)
        }
        val customPaths = runCatching {
            val array = JSONArray(preferences.getString(KEY_CUSTOM_PATHS, "[]"))
            buildList {
                for (index in 0 until array.length()) add(array.getString(index))
            }
        }.getOrDefault(emptyList())
        val custom = customPaths.filterNot { path -> DemoWorkspace.files.any { it.path == path } }.map { path ->
            WorkspaceFile(
                path = path,
                language = path.substringAfterLast('.', "Plain text"),
                content = preferences.getString(fileKey(path), null) ?: "",
            )
        }
        return defaults + custom
    }

    fun saveDemoFile(file: WorkspaceFile) {
        val editor = preferences.edit().putString(fileKey(file.path), file.content)
        if (DemoWorkspace.files.none { it.path == file.path }) {
            val paths = loadDemoFiles().map { it.path }.filter { path -> DemoWorkspace.files.none { it.path == path } }
            editor.putString(KEY_CUSTOM_PATHS, JSONArray((paths + file.path).distinct()).toString())
        }
        editor.apply()
    }

    fun resetDemoFiles() {
        val customPaths = loadDemoFiles().map { it.path }.filter { path -> DemoWorkspace.files.none { it.path == path } }
        val editor = preferences.edit().remove(KEY_CUSTOM_PATHS)
        customPaths.forEach { editor.remove(fileKey(it)) }
        DemoWorkspace.files.forEach { file ->
            editor.putString(fileKey(file.path), file.content)
        }
        editor.apply()
    }

    private fun fileKey(path: String) = "demo_file:$path"

    private companion object {
        const val KEY_SERVERS = "servers"
        const val KEY_ACTIVE_SERVER = "active_server"
        const val KEY_ENTERED = "entered_workspace"
        const val KEY_CUSTOM_PATHS = "custom_demo_paths"
    }
}
