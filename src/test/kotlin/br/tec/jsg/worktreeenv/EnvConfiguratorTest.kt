package br.tec.jsg.worktreeenv

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EnvConfiguratorTest {

    @TempDir
    lateinit var tmp: File

    @Test
    fun `replaceUrlHost preserves scheme and TLD`() {
        val result = EnvConfigurator.replaceUrlHost(
            "http://myapp.test", "myapp", "myapp-feature",
        )
        assertEquals("http://myapp-feature.test", result)
    }

    @Test
    fun `replaceUrlHost preserves compound TLD`() {
        val result = EnvConfigurator.replaceUrlHost(
            "http://myapp.dev.br", "myapp", "myapp-feature",
        )
        assertEquals("http://myapp-feature.dev.br", result)
    }

    @Test
    fun `replaceUrlHost preserves port`() {
        val result = EnvConfigurator.replaceUrlHost(
            "http://myapp.dev.br:8080/api", "myapp", "myapp-feature",
        )
        assertEquals("http://myapp-feature.dev.br:8080/api", result)
    }

    @Test
    fun `replaceUrlHost preserves https`() {
        val result = EnvConfigurator.replaceUrlHost(
            "https://myapp.herd.local", "myapp", "myapp-feature",
        )
        assertEquals("https://myapp-feature.herd.local", result)
    }

    @Test
    fun `replaceUrlHost handles malformed URL with fallback`() {
        val result = EnvConfigurator.replaceUrlHost(
            "not-a-url", "myapp", "myapp-feature",
        )
        assertEquals("http://myapp-feature.test", result)
    }

    @Test
    fun `configure copies env and updates APP_URL in auto-detect mode`() {
        val mainDir = File(tmp, "myapp").apply { mkdirs() }
        val wtDir = File(tmp, "myapp-feature").apply { mkdirs() }

        File(mainDir, ".env").writeText(
            "APP_NAME=MyApp\nAPP_URL=http://myapp.test\nAPP_KEY=base64:xxx\nDB_HOST=127.0.0.1",
        )

        val info = WorktreeInfo(
            worktreeRoot = wtDir,
            mainRoot = mainDir,
            worktreeFolderName = "myapp-feature",
            mainFolderName = "myapp",
        )

        val result = EnvConfigurator.configure(info, appUrlPattern = "", copyTesting = false)

        assertTrue(result.success)
        assertEquals("http://myapp-feature.test", result.newAppUrl)
        assertFalse(result.testingConfigured)

        val destEnv = File(wtDir, ".env")
        assertTrue(destEnv.exists())
        val lines = destEnv.readLines()
        assertEquals("APP_NAME=MyApp", lines[0])
        assertEquals("APP_URL=http://myapp-feature.test", lines[1])
        assertEquals("APP_KEY=base64:xxx", lines[2])
        assertEquals("DB_HOST=127.0.0.1", lines[3])
    }

    @Test
    fun `configure uses pattern when provided`() {
        val mainDir = File(tmp, "myapp").apply { mkdirs() }
        val wtDir = File(tmp, "myapp-feature").apply { mkdirs() }

        File(mainDir, ".env").writeText("APP_URL=http://myapp.test")

        val info = WorktreeInfo(
            worktreeRoot = wtDir,
            mainRoot = mainDir,
            worktreeFolderName = "myapp-feature",
            mainFolderName = "myapp",
        )

        val result = EnvConfigurator.configure(
            info,
            appUrlPattern = "https://{folder}.local:8443",
            copyTesting = false,
        )

        assertTrue(result.success)
        assertEquals("https://myapp-feature.local:8443", result.newAppUrl)
    }

    @Test
    fun `configure returns error when source env does not exist`() {
        val mainDir = File(tmp, "myapp").apply { mkdirs() }
        val wtDir = File(tmp, "myapp-feature").apply { mkdirs() }

        val info = WorktreeInfo(
            worktreeRoot = wtDir,
            mainRoot = mainDir,
            worktreeFolderName = "myapp-feature",
            mainFolderName = "myapp",
        )

        val result = EnvConfigurator.configure(info, appUrlPattern = "", copyTesting = false)

        assertFalse(result.success)
        assertTrue(result.error.contains("Source .env not found"))
    }

    @Test
    fun `configure copies env testing when requested`() {
        val mainDir = File(tmp, "myapp").apply { mkdirs() }
        val wtDir = File(tmp, "myapp-feature").apply { mkdirs() }

        File(mainDir, ".env").writeText("APP_URL=http://myapp.test\nDB_HOST=127.0.0.1")
        File(mainDir, ".env.testing").writeText("APP_URL=http://myapp.test\nDB_HOST=sqlite")

        val info = WorktreeInfo(
            worktreeRoot = wtDir,
            mainRoot = mainDir,
            worktreeFolderName = "myapp-feature",
            mainFolderName = "myapp",
        )

        val result = EnvConfigurator.configure(info, appUrlPattern = "", copyTesting = true)

        assertTrue(result.success)
        assertTrue(result.testingConfigured)

        val testingEnv = File(wtDir, ".env.testing")
        assertTrue(testingEnv.exists())
        assertTrue(testingEnv.readText().contains("APP_URL=http://myapp-feature.test"))
        assertTrue(testingEnv.readText().contains("DB_HOST=sqlite"))
    }

    @Test
    fun `readEnvValue handles quoted values`() {
        val envFile = File(tmp, ".env").apply {
            writeText("APP_NAME=\"My App\"\nAPP_KEY='base64:secret'")
        }

        assertEquals("My App", EnvConfigurator.readEnvValue(envFile, "APP_NAME"))
        assertEquals("base64:secret", EnvConfigurator.readEnvValue(envFile, "APP_KEY"))
    }

    @Test
    fun `readEnvValue returns null for missing key`() {
        val envFile = File(tmp, ".env").apply {
            writeText("APP_NAME=Test")
        }

        assertNull(EnvConfigurator.readEnvValue(envFile, "MISSING_KEY"))
    }
}
