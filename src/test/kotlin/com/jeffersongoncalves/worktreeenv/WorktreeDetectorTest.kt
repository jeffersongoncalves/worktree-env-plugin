package com.jeffersongoncalves.worktreeenv

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WorktreeDetectorTest {

    @Rule
    @JvmField
    val tmp = TemporaryFolder()

    @Test
    fun `returns null when git is a directory (main project)`() {
        val project = File(tmp.root, "myapp").apply { mkdirs() }
        val gitDir = File(project, ".git").apply { mkdirs() }
        File(gitDir, "HEAD").writeText("ref: refs/heads/main")

        val result = WorktreeDetector.detect(project)
        assertNull(result)
    }

    @Test
    fun `returns null when git is absent`() {
        val project = File(tmp.root, "myapp").apply { mkdirs() }

        val result = WorktreeDetector.detect(project)
        assertNull(result)
    }

    @Test
    fun `returns null when git file has no gitdir prefix`() {
        val project = File(tmp.root, "myapp").apply { mkdirs() }
        File(project, ".git").writeText("something else")

        val result = WorktreeDetector.detect(project)
        assertNull(result)
    }

    @Test
    fun `detects worktree when git points to commondir with valid HEAD`() {
        // Setup main project
        val mainProject = File(tmp.root, "myapp").apply { mkdirs() }
        val mainGitDir = File(mainProject, ".git").apply { mkdirs() }
        File(mainGitDir, "HEAD").writeText("ref: refs/heads/main")

        // Setup worktrees directory inside main .git
        val worktreesDir = File(mainGitDir, "worktrees/feature-payment").apply { mkdirs() }
        File(worktreesDir, "commondir").writeText("../..")
        File(worktreesDir, "HEAD").writeText("ref: refs/heads/feature-payment")

        // Setup worktree project directory
        val worktreeProject = File(tmp.root, "myapp-feature-payment").apply { mkdirs() }
        File(worktreeProject, ".git").writeText("gitdir: ${worktreesDir.absolutePath}")

        val result = WorktreeDetector.detect(worktreeProject)
        assertNotNull(result)
        assertEquals("myapp-feature-payment", result!!.worktreeFolderName)
        assertEquals("myapp", result.mainFolderName)
        assertEquals(mainProject.canonicalPath, result.mainRoot.canonicalPath)
        assertEquals(worktreeProject.canonicalPath, result.worktreeRoot.canonicalPath)
    }

    @Test
    fun `isEnvAlreadyConfigured returns false when env does not exist`() {
        val info = WorktreeInfo(
            worktreeRoot = File(tmp.root, "wt").apply { mkdirs() },
            mainRoot = File(tmp.root, "main").apply { mkdirs() },
            worktreeFolderName = "myapp-feature",
            mainFolderName = "myapp",
        )

        assertFalse(WorktreeDetector.isEnvAlreadyConfigured(info))
    }

    @Test
    fun `isEnvAlreadyConfigured returns true when APP_URL contains folder name`() {
        val wtDir = File(tmp.root, "myapp-feature").apply { mkdirs() }
        File(wtDir, ".env").writeText("APP_NAME=Test\nAPP_URL=http://myapp-feature.test\nAPP_KEY=xxx")

        val info = WorktreeInfo(
            worktreeRoot = wtDir,
            mainRoot = File(tmp.root, "myapp").apply { mkdirs() },
            worktreeFolderName = "myapp-feature",
            mainFolderName = "myapp",
        )

        assertTrue(WorktreeDetector.isEnvAlreadyConfigured(info))
    }

    @Test
    fun `isEnvAlreadyConfigured returns false when APP_URL uses main project name`() {
        val wtDir = File(tmp.root, "myapp-feature").apply { mkdirs() }
        File(wtDir, ".env").writeText("APP_NAME=Test\nAPP_URL=http://myapp.test\nAPP_KEY=xxx")

        val info = WorktreeInfo(
            worktreeRoot = wtDir,
            mainRoot = File(tmp.root, "myapp").apply { mkdirs() },
            worktreeFolderName = "myapp-feature",
            mainFolderName = "myapp",
        )

        assertFalse(WorktreeDetector.isEnvAlreadyConfigured(info))
    }
}
