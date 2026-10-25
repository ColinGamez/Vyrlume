package app.vyrlume.mobile.util

import java.net.URI

object ServerUrl {
    fun normalize(raw: String): Result<String> = runCatching {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Enter a server address." }
        require(!trimmed.any { it.isWhitespace() }) { "The address cannot contain spaces." }
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val uri = URI(withScheme)
        require(uri.scheme == "http" || uri.scheme == "https") { "Only HTTP and HTTPS addresses are supported." }
        require(!uri.host.isNullOrBlank()) { "Enter a valid host name or IP address." }
        require(uri.userInfo == null) { "Put credentials in the server sign-in page, not the URL." }
        require(uri.fragment == null) { "Remove the URL fragment." }
        withScheme.trimEnd('/')
    }

    fun previewUrl(serverUrl: String, port: Int): String {
        val uri = URI(serverUrl)
        val base = URI(uri.scheme, null, uri.host, uri.port, uri.path, null, null)
            .toString()
            .trimEnd('/')
        return "$base/proxy/$port/"
    }
}
