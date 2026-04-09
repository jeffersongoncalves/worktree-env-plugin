package com.jeffersongoncalves.worktreeenv.ui

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.Consumer
import com.jeffersongoncalves.worktreeenv.WorktreeEnvService
import com.jeffersongoncalves.worktreeenv.WorktreeStatus
import com.jeffersongoncalves.worktreeenv.icons.WorktreeEnvIcons
import java.awt.event.MouseEvent
import javax.swing.Icon

class WorktreeEnvStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "Worktree Env"

    override fun isAvailable(project: Project): Boolean {
        return WorktreeEnvService.getInstance(project).status != WorktreeStatus.NOT_WORKTREE
    }

    override fun createWidget(project: Project): StatusBarWidget = WorktreeEnvStatusBarWidget(project)

    companion object {
        const val WIDGET_ID = "WorktreeEnvStatusBarWidget"
    }
}

private class WorktreeEnvStatusBarWidget(private val project: Project) :
    StatusBarWidget,
    StatusBarWidget.IconPresentation {

    private var statusBar: StatusBar? = null

    override fun ID(): String = WorktreeEnvStatusBarWidgetFactory.WIDGET_ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        project.messageBus.connect().subscribe(
            WorktreeEnvService.STATUS_CHANGED,
            WorktreeEnvService.StatusChangeListener {
                invokeLater { statusBar.updateWidget(ID()) }
            },
        )
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getIcon(): Icon {
        val service = WorktreeEnvService.getInstance(project)
        return when (service.status) {
            WorktreeStatus.CONFIGURED -> WorktreeEnvIcons.CONFIGURED
            WorktreeStatus.UNCONFIGURED -> WorktreeEnvIcons.UNCONFIGURED
            WorktreeStatus.NOT_WORKTREE -> WorktreeEnvIcons.WORKTREE
        }
    }

    override fun getTooltipText(): String {
        val service = WorktreeEnvService.getInstance(project)
        return when (service.status) {
            WorktreeStatus.CONFIGURED -> "Worktree .env: ${service.currentAppUrl}"
            WorktreeStatus.UNCONFIGURED -> "Worktree .env: Not configured — click to open"
            WorktreeStatus.NOT_WORKTREE -> "Not a Git worktree"
        }
    }

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Worktree Env")
        toolWindow?.show()
    }
}
