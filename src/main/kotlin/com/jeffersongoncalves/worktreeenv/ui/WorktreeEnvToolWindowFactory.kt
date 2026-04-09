package com.jeffersongoncalves.worktreeenv.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.jeffersongoncalves.worktreeenv.WorktreeEnvService
import com.jeffersongoncalves.worktreeenv.WorktreeStatus

class WorktreeEnvToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun shouldBeAvailable(project: Project): Boolean {
        return WorktreeEnvService.getInstance(project).status != WorktreeStatus.NOT_WORKTREE
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = WorktreeEnvToolWindowPanel(project)
        val content = toolWindow.contentManager.factory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
