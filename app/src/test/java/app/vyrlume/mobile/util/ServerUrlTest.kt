package app.vyrlume.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlTest {
    @Test
    fun `adds HTTPS and removes trailing slashes`() {
        assertEquals("https://code.example.com", ServerUrl.normalize("code.example.com///").getOrThrow())
    }

    @Test
    fun `keeps explicit local HTTP`() {
        assertEquals("http://192.168.1.20:8080", ServerUrl.normalize("http://192.168.1.20:8080/").getOrThrow())
    }

    @Test
    fun `rejects embedded credentials`() {
        val result = ServerUrl.normalize("https://user:secret@example.com")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("credentials") == true)
    }

    @Test
    fun `rejects unsupported schemes`() {
        assertTrue(ServerUrl.normalize("file:///sdcard/project").isFailure)
    }

    @Test
    fun `builds code-server proxy preview URL`() {
        assertEquals(
            "https://code.example.com/proxy/3000/",
            ServerUrl.previewUrl("https://code.example.com/", 3000),
        )
        assertEquals(
            "https://code.example.com:8443/workspace/proxy/5173/",
            ServerUrl.previewUrl("https://code.example.com:8443/workspace/?folder=/home/dev", 5173),
        )
    }
}
