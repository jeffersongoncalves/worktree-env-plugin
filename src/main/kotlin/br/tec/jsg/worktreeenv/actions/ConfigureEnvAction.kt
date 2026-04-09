package br.tec.jsg.worktreeenv.actions

import br.tec.jsg.worktreeenv.WorktreeDetector
import br.tec.jsg.worktreeenv.WorktreeEnvStartupActivity
import br.tec.jsg.worktreeenv.settings.WorktreeEnvSettings
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class ConfigureEnvAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val info = WorktreeDetector.detect(project)
        if (info == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.isEnabledAndVisible = true
        e.presentation.text = "Configure .env for Worktree '${info.worktreeFolderName}'"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val info = WorktreeDetector.detect(project)
        if (info == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("WorktreeEnvConfigurator")
                .createNotification(
                    "Not a Worktree",
                    "This project does not appear to be a Git worktree.",
                    NotificationType.WARNING,
                )
                .notify(project)
            return
        }

        val settings = WorktreeEnvSettings.getInstance()
        WorktreeEnvStartupActivity().runConfiguration(project, info, settings)
    }
}
