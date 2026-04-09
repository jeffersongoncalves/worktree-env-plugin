# Worktree Env Configurator

PHPStorm plugin that automatically detects Git worktree projects and configures `.env` files by copying from the main project and adjusting `APP_URL`.

## Problem

When using Git worktrees with Laravel projects, each worktree needs its own `.env` with `APP_URL` pointing to the correct local hostname (e.g., `http://myapp-feature-payment.test`). This plugin automates that process.

## Features

- **Auto-detection**: Detects when a project is opened as a Git worktree
- **Smart APP_URL**: Preserves scheme, TLD, port, and path from the original URL
- **Configurable pattern**: Use `{folder}` placeholder for custom URL patterns
- **`.env.testing` support**: Optionally copies and configures the testing env
- **Non-intrusive**: Shows a balloon notification with "Configure" and "Ignore" options
- **Manual action**: Available via `Tools → Configure Worktree .env`

## How It Works

1. On project open, checks if `.git` is a file (worktree indicator)
2. Resolves the main project path via `gitdir:` and `commondir`
3. If `.env` is not yet configured, shows a notification
4. On "Configure .env": copies `.env` from the main project, replaces `APP_URL`

### APP_URL Resolution

| Main Project URL | Worktree Folder | Result |
|---|---|---|
| `http://myapp.test` | `myapp-feature` | `http://myapp-feature.test` |
| `https://myapp.herd.local` | `myapp-feature` | `https://myapp-feature.herd.local` |
| `http://myapp.dev.br:8080/api` | `myapp-feature` | `http://myapp-feature.dev.br:8080/api` |

## Settings

`Settings → Tools → Worktree Env Configurator`

| Setting | Default | Description |
|---|---|---|
| Auto-configure on open | `true` | Show notification when worktree is detected |
| Copy .env.testing | `true` | Also configure `.env.testing` if it exists |
| Open in editor | `true` | Open `.env` after configuration |
| Pattern | (empty) | Custom URL pattern, e.g. `http://{folder}.test` |

## Build

```bash
./gradlew build
```

The plugin ZIP will be at `build/distributions/`.

## Test

```bash
./gradlew test
```

## Install

1. Build the plugin
2. PHPStorm → Settings → Plugins → ⚙️ → Install Plugin from Disk
3. Select the ZIP from `build/distributions/`

## Requirements

- PHPStorm 2023.2 — 2025.1
- Git4Idea plugin (bundled)
- PHP plugin (bundled in PHPStorm)
