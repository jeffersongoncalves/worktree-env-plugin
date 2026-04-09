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
        val gitdirFile = if (File(gitdirPath).isAbsolute) {
            File(gitdirPath).canonicalFile
        } else {
            File(baseDir, gitdirPath).canonicalFile
        }

        val mainGitDir = resolveMainGitDir(gitdirFile)
            ?: resolveMainGitDirByTraversal(baseDir)
            ?: return null

        val mainRoot = mainGitDir.parentFile?.canonicalFile ?: return null
        val worktreeRoot = baseDir.canonicalFile

        if (worktreeRoot == mainRoot) return null

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

        val commondirPath = commondirFile.readText().trim()
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

    fun isEnvAlreadyConfigured(info: WorktreeInfo): Boolean {
        val envFile = File(info.worktreeRoot, ".env")
        if (!envFile.exists()) return false

        val appUrl = EnvConfigurator.readEnvValue(envFile, "APP_URL") ?: return false
        return appUrl.contains(info.worktreeFolderName)
    }
}
