package br.tec.jsg.worktreeenv

import java.io.File
import java.net.URI

data class ConfigureResult(
    val success: Boolean,
    val newAppUrl: String = "",
    val testingConfigured: Boolean = false,
    val error: String = "",
)

object EnvConfigurator {

    fun configure(
        info: WorktreeInfo,
        appUrlPattern: String,
        copyTesting: Boolean,
    ): ConfigureResult {
        val sourceEnv = File(info.mainRoot, ".env")
        if (!sourceEnv.exists()) {
            return ConfigureResult(success = false, error = "Source .env not found at ${info.mainRoot}")
        }

        val newAppUrl = resolveAppUrl(info, appUrlPattern, sourceEnv)
        val destEnv = File(info.worktreeRoot, ".env")
        copyEnvWithAppUrl(sourceEnv, destEnv, newAppUrl)

        var testingConfigured = false
        if (copyTesting) {
            val sourceTesting = File(info.mainRoot, ".env.testing")
            if (sourceTesting.exists()) {
                val destTesting = File(info.worktreeRoot, ".env.testing")
                copyEnvWithAppUrl(sourceTesting, destTesting, newAppUrl)
                testingConfigured = true
            }
        }

        return ConfigureResult(
            success = true,
            newAppUrl = newAppUrl,
            testingConfigured = testingConfigured,
        )
    }

    private fun resolveAppUrl(info: WorktreeInfo, pattern: String, sourceEnv: File): String {
        if (pattern.isNotBlank()) {
            return pattern.replace("{folder}", info.worktreeFolderName)
        }

        val currentUrl = readEnvValue(sourceEnv, "APP_URL")
            ?: return "http://${info.worktreeFolderName}.test"

        return replaceUrlHost(currentUrl, info.mainFolderName, info.worktreeFolderName)
    }

    @Suppress("UNUSED_PARAMETER")
    internal fun replaceUrlHost(url: String, oldHost: String, newHost: String): String {
        return try {
            val uri = URI(url)
            val hostname = uri.host ?: return "http://$newHost.test"

            val dotIndex = hostname.indexOf('.')
            if (dotIndex < 0) {
                val newUri = buildUri(uri, newHost)
                return newUri
            }

            val tld = hostname.substring(dotIndex)
            val newHostname = "$newHost$tld"
            buildUri(uri, newHostname)
        } catch (_: Exception) {
            "http://$newHost.test"
        }
    }

    private fun buildUri(original: URI, newHost: String): String {
        val sb = StringBuilder()
        sb.append(original.scheme ?: "http")
        sb.append("://")
        sb.append(newHost)
        if (original.port > 0) {
            sb.append(":${original.port}")
        }
        if (!original.path.isNullOrEmpty()) {
            sb.append(original.path)
        }
        return sb.toString()
    }

    private fun copyEnvWithAppUrl(source: File, dest: File, newAppUrl: String) {
        val lines = source.readLines()
        val updatedLines = lines.map { line ->
            if (line.startsWith("APP_URL=")) {
                "APP_URL=$newAppUrl"
            } else {
                line
            }
        }
        dest.writeText(updatedLines.joinToString("\n"))
    }

    fun readEnvValue(envFile: File, key: String): String? {
        if (!envFile.exists()) return null

        for (line in envFile.readLines()) {
            if (line.startsWith("$key=")) {
                val value = line.removePrefix("$key=").trim()
                return value
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
            }
        }
        return null
    }
}
