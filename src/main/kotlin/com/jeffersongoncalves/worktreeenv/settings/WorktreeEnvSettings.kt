package com.jeffersongoncalves.worktreeenv.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "WorktreeEnvSettings",
    storages = [Storage("WorktreeEnvSettings.xml")],
)
class WorktreeEnvSettings : PersistentStateComponent<WorktreeEnvSettings.State> {

    data class State(
        var autoConfigureOnOpen: Boolean = true,
        var appUrlPattern: String = "",
        var copyTestingEnv: Boolean = true,
        var openEnvInEditor: Boolean = true,
        var ignoredProjects: MutableList<String> = mutableListOf(),
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var autoConfigureOnOpen: Boolean
        get() = myState.autoConfigureOnOpen
        set(value) { myState.autoConfigureOnOpen = value }

    var appUrlPattern: String
        get() = myState.appUrlPattern
        set(value) { myState.appUrlPattern = value }

    var copyTestingEnv: Boolean
        get() = myState.copyTestingEnv
        set(value) { myState.copyTestingEnv = value }

    var openEnvInEditor: Boolean
        get() = myState.openEnvInEditor
        set(value) { myState.openEnvInEditor = value }

    var ignoredProjects: MutableList<String>
        get() = myState.ignoredProjects
        set(value) { myState.ignoredProjects = value }

    fun isIgnored(projectBasePath: String): Boolean {
        return ignoredProjects.contains(projectBasePath)
    }

    fun addIgnored(projectBasePath: String) {
        if (!ignoredProjects.contains(projectBasePath)) {
            ignoredProjects.add(projectBasePath)
        }
    }

    companion object {
        fun getInstance(): WorktreeEnvSettings {
            return ApplicationManager.getApplication().getService(WorktreeEnvSettings::class.java)
        }
    }
}
