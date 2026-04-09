package com.jeffersongoncalves.worktreeenv.ui

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jeffersongoncalves.worktreeenv.EnvConfigurator
import com.jeffersongoncalves.worktreeenv.WorktreeEnvService
import com.jeffersongoncalves.worktreeenv.WorktreeStatus
import com.jeffersongoncalves.worktreeenv.icons.WorktreeEnvIcons
import com.jeffersongoncalves.worktreeenv.settings.WorktreeEnvSettings
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSeparator

class WorktreeEnvToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val service = WorktreeEnvService.getInstance(project)

    private val statusIcon = JBLabel()
    private val statusText = JBLabel()
    private val urlLabel = JBLabel()
    private val worktreeLabel = JBLabel()
    private val mainProjectLabel = JBLabel()
    private val configureButton = JButton("Configure .env")
    private val openEnvButton = JButton("Open .env in Editor")
    private val refreshButton = JButton("Refresh")

    init {
        border = JBUI.Borders.empty(12)
        buildUI()
        updateStatus()

        project.messageBus.connect().subscribe(
            WorktreeEnvService.STATUS_CHANGED,
            WorktreeEnvService.StatusChangeListener {
                invokeLater { updateStatus() }
            },
        )

        configureButton.addActionListener { onConfigure() }
        openEnvButton.addActionListener { onOpenEnv() }
        refreshButton.addActionListener { onRefresh() }
    }

    private fun buildUI() {
        val content = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0
            gridy = 0
        }

        // Status section
        content.add(createSectionLabel("Status"), gbc)
        gbc.gridy++

        val statusRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))
        statusRow.add(statusIcon)
        statusRow.add(statusText)
        content.add(statusRow, gbc)
        gbc.gridy++

        val urlRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))
        urlRow.add(JBLabel("APP_URL:"))
        urlRow.add(urlLabel)
        content.add(urlRow, gbc)
        gbc.gridy++

        content.add(JSeparator(), gbc.apply { insets = JBUI.insets(8, 0) })
        gbc.gridy++
        gbc.insets = JBUI.emptyInsets()

        // Worktree info section
        content.add(createSectionLabel("Worktree Info"), gbc)
        gbc.gridy++

        val wtRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))
        wtRow.add(JBLabel("Worktree:"))
        wtRow.add(worktreeLabel)
        content.add(wtRow, gbc)
        gbc.gridy++

        val mainRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))
        mainRow.add(JBLabel("Main Project:"))
        mainRow.add(mainProjectLabel)
        content.add(mainRow, gbc)
        gbc.gridy++

        content.add(JSeparator(), gbc.apply { insets = JBUI.insets(8, 0) })
        gbc.gridy++
        gbc.insets = JBUI.emptyInsets()

        // Actions section
        content.add(createSectionLabel("Actions"), gbc)
        gbc.gridy++

        val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4))
        actionsPanel.add(configureButton)
        actionsPanel.add(openEnvButton)
        actionsPanel.add(refreshButton)
        content.add(actionsPanel, gbc)
        gbc.gridy++

        // Spacer
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        content.add(JPanel(), gbc)

        add(content, BorderLayout.CENTER)
    }

    private fun createSectionLabel(text: String): JBLabel {
        val label = JBLabel(text)
        label.font = label.font.deriveFont(java.awt.Font.BOLD)
        label.border = BorderFactory.createEmptyBorder(4, 0, 4, 0)
        return label
    }

    private fun updateStatus() {
        val info = service.info

        when (service.status) {
            WorktreeStatus.CONFIGURED -> {
                statusIcon.icon = WorktreeEnvIcons.CONFIGURED
                statusText.text = "Configured"
                statusText.foreground = JBColor(Color(39, 174, 96), Color(46, 204, 113))
                urlLabel.text = service.currentAppUrl ?: "N/A"
                configureButton.text = "Reconfigure .env"
                configureButton.isEnabled = true
                openEnvButton.isEnabled = true
            }
            WorktreeStatus.UNCONFIGURED -> {
                statusIcon.icon = WorktreeEnvIcons.UNCONFIGURED
                statusText.text = "Not Configured"
                statusText.foreground = JBColor(Color(211, 84, 0), Color(230, 126, 34))
                urlLabel.text = service.currentAppUrl ?: "N/A"
                configureButton.text = "Configure .env"
                configureButton.isEnabled = true
                openEnvButton.isEnabled = File(info?.worktreeRoot ?: return, ".env").exists()
            }
            WorktreeStatus.NOT_WORKTREE -> {
                statusIcon.icon = WorktreeEnvIcons.WORKTREE
                statusText.text = "Not a Worktree"
                statusText.foreground = JBColor.GRAY
                urlLabel.text = "N/A"
                configureButton.isEnabled = false
                openEnvButton.isEnabled = false
            }
        }

        if (info != null) {
            worktreeLabel.text = info.worktreeFolderName
            mainProjectLabel.text = info.mainFolderName
        } else {
            worktreeLabel.text = "N/A"
            mainProjectLabel.text = "N/A"
        }
    }

    private fun onConfigure() {
        val info = service.info ?: return
        val settings = WorktreeEnvSettings.getInstance()

        val result = EnvConfigurator.configure(
            info = info,
            appUrlPattern = settings.appUrlPattern,
            copyTesting = settings.copyTestingEnv,
        )

        VirtualFileManager.getInstance().asyncRefresh {}

        if (result.success) {
            service.refresh()
            if (settings.openEnvInEditor) {
                onOpenEnv()
            }
        }
    }

    private fun onOpenEnv() {
        val info = service.info ?: return
        val envFile = File(info.worktreeRoot, ".env")
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            com.intellij.openapi.application.WriteAction.run<Throwable> {
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(envFile) ?: return@run
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
        }
    }

    private fun onRefresh() {
        service.refresh()
    }
}
