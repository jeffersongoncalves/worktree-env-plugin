package br.tec.jsg.worktreeenv.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel

class WorktreeEnvConfigurable : BoundConfigurable("Worktree Env Configurator") {

    private val settings = WorktreeEnvSettings.getInstance()

    override fun createPanel(): DialogPanel = panel {
        group("Behavior") {
            row {
                checkBox("Auto-configure .env when a worktree project is opened")
                    .bindSelected(settings::autoConfigureOnOpen)
            }
            row {
                checkBox("Also copy and configure .env.testing")
                    .bindSelected(settings::copyTestingEnv)
            }
            row {
                checkBox("Open .env in editor after configuration")
                    .bindSelected(settings::openEnvInEditor)
            }
        }

        group("APP_URL Configuration") {
            row("Pattern:") {
                textField()
                    .bindText(settings::appUrlPattern)
                    .columns(COLUMNS_LARGE)
                    .comment(
                        "Use <code>{folder}</code> as placeholder for the worktree folder name.<br>" +
                        "Example: <code>http://{folder}.test</code><br>" +
                        "Leave empty to auto-detect from the main project's .env"
                    )
            }
        }

        group("Ignored Projects") {
            row {
                comment("Projects you chose to ignore will not trigger automatic .env configuration.")
            }
            row {
                button("Reset Ignored Projects") {
                    settings.ignoredProjects.clear()
                }
            }
        }
    }
}
