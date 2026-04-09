package com.jeffersongoncalves.worktreeenv

import com.intellij.openapi.project.Project
import java.io.File

data class WorktreeInfo(
    val worktreeRoot: File,
    val mainRoot: File,
    val worktreeFolderName: String,
    val mainFolderName: String,
)

object WorktreeDetector {

    fun detect(project: Project): WorktreeInfo? {
        val basePath = project.basePath ?: return null
        return detect(File(basePath))
    }

    fun detect(baseDir: File): WorktreeInfo? {
        val gitEntry = File(baseDir, ".git")

        if (!gitEntry.exists()) return null
        if (gitEntry.isDirectory) return null

        val content = gitEntry.readText().trim()
        if (!content.startsWith("gitdir:")) return null

        val gitdirPath = content.removePrefix("gitdir:").trim()
        val normalizedPath = gitdirPath.replace("\\", "/")
        val gitdirFile = if (File(normalizedPath).isAbsolute) {
            File(normalizedPath).canonicalFile
        } else {
            File(baseDir, normalizedPath).canonicalFile
        }

        val mainGitDir = resolveMainGitDir(gitdirFile)
            ?: resolveMainGitDirByTraversal(baseDir)
            ?: return null

        val mainRoot = mainGitDir.parentFile?.canonicalFile ?: return null
        val worktreeRoot = baseDir.canonicalFile

        if (pathsEqual(worktreeRoot, mainRoot)) return null

        return WorktreeInfo(
            worktreeRoot = worktreeRoot,
            mainRoot = mainRoot,
            worktreeFolderName = worktreeRoot.name,
            mainFolderName = mainRoot.name,
        )
    }

    private fun resolveMainGitDir(gitdirFile: File): File? {
        val commondirFile = File(gitdirFile, "commondir")
        if (!commondirFile.exists()) return null

        val commondirPath = commondirFile.readText().trim().replace("\\", "/")
        val mainGitDir = File(gitdirFile, commondirPath).canonicalFile

        return if (File(mainGitDir, "HEAD").exists()) mainGitDir else null
    }

    private fun resolveMainGitDirByTraversal(startDir: File): File? {
        var current = startDir.canonicalFile.parentFile
        while (current != null) {
            val gitDir = File(current, ".git")
            if (gitDir.isDirectory && File(gitDir, "HEAD").exists()) {
                return gitDir
            }
            current = current.parentFile
        }
        return null
    }

    /**
     * Compare two paths in a cross-platform way.
     * Uses canonicalPath string comparison (case-insensitive on Windows,
     * case-sensitive on macOS/Linux) via the canonical form.
     */
    private fun pathsEqual(a: File, b: File): Boolean {
        return a.canonicalPath == b.canonicalPath
    }

    fun isEnvAlreadyConfigured(info: WorktreeInfo): Boolean {
        val envFile = File(info.worktreeRoot, ".env")
        if (!envFile.exists()) return false

        val appUrl = EnvConfigurator.readEnvValue(envFile, "APP_URL") ?: return false
        return appUrl.lowercase().contains(info.worktreeFolderName.lowercase())
    }
}
