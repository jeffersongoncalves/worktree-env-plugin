package com.jeffersongoncalves.worktreeenv.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object WorktreeEnvIcons {
    @JvmField val WORKTREE: Icon = IconLoader.getIcon("/icons/worktree.svg", WorktreeEnvIcons::class.java)
    @JvmField val CONFIGURED: Icon = IconLoader.getIcon("/icons/worktree_configured.svg", WorktreeEnvIcons::class.java)
    @JvmField val UNCONFIGURED: Icon = IconLoader.getIcon("/icons/worktree_unconfigured.svg", WorktreeEnvIcons::class.java)
}
