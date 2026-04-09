package br.tec.jsg.worktreeenv

import br.tec.jsg.worktreeenv.settings.WorktreeEnvSettings
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

class WorktreeEnvStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val settings = WorktreeEnvSettings.getInstance()
        val basePath = project.basePath ?: return

        if (!settings.autoConfigureOnOpen) return
        if (settings.isIgnored(basePath)) return

        val info = WorktreeDetector.detect(project) ?: return
        if (WorktreeDetector.isEnvAlreadyConfigured(info)) return

        val sourceEnv = File(info.mainRoot, ".env")
        if (!sourceEnv.exists()) return

        val notification = notificationGroup()
            .createNotification(
                "Git Worktree Detected",
                "Worktree <b><code>${info.worktreeFolderName}</code></b> " +
                    "from main project <b><code>${info.mainFolderName}</code></b>",
                NotificationType.INFORMATION,
            )
            .addAction(
                object : com.intellij.notification.NotificationAction("Configure .env") {
                    override fun actionPerformed(
                        e: com.intellij.openapi.actionSystem.AnActionEvent,
                        notification: com.intellij.notification.Notification,
                    ) {
                        notification.expire()
                        runConfiguration(project, info, settings)
                    }
                },
            )
            .addAction(
                object : com.intellij.notification.NotificationAction("Ignore this project") {
                    override fun actionPerformed(
                        e: com.intellij.openapi.actionSystem.AnActionEvent,
                        notification: com.intellij.notification.Notification,
                    ) {
                        notification.expire()
                        settings.addIgnored(basePath)
                    }
                },
            )

        notification.notify(project)
    }

    fun runConfiguration(project: Project, info: WorktreeInfo, settings: WorktreeEnvSettings) {
        val result = EnvConfigurator.configure(
            info = info,
            appUrlPattern = settings.appUrlPattern,
            copyTesting = settings.copyTestingEnv,
        )

        if (!result.success) {
            notificationGroup()
                .createNotification(
                    "Worktree Env Configuration Failed",
                    result.error,
                    NotificationType.ERROR,
                )
                .notify(project)
            return
        }

        VirtualFileManager.getInstance().asyncRefresh {}

        if (settings.openEnvInEditor) {
            val envFile = File(info.worktreeRoot, ".env")
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(envFile)
            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
        }

        val testingMsg = if (result.testingConfigured) " (.env.testing also configured)" else ""
        notificationGroup()
            .createNotification(
                "Worktree Env Configured",
                "APP_URL set to <code>${result.newAppUrl}</code>$testingMsg",
                NotificationType.INFORMATION,
            )
            .notify(project)
    }

    private fun notificationGroup() =
        NotificationGroupManager.getInstance().getNotificationGroup("WorktreeEnvConfigurator")
}
