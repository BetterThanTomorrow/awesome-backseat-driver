# Awesome Backseat Driver — Workspace Instructions

## Project Structure

This is a VS Code Copilot plugin marketplace repo — pure content (markdown, JSON, PNG). No build step.

- `.github/plugin/marketplace.json` — plugin registry, version source of truth
- `plugins/<name>/` — individual plugins, each with `.github/plugin/plugin.json`
- `instructions/` — standalone instruction files (not bundled in plugins)

## Changelog

Maintain `CHANGELOG.md` using [Keep a Changelog](https://keepachangelog.com/) format. Only plugin-related changes get entries — new plugins, removed plugins, updated skills/agents, and changed instructions. Infrastructure changes (CI, scripts, README generation) do not belong in the changelog.

- Add entries under `## [Unreleased]` before committing.
- Entries are bullet points. Link issues when relevant.
- The publish pipeline moves unreleased entries to a versioned section automatically — just keep `## [Unreleased]` tended.

## Validation

Run `bb validate` to check plugin structure: JSON validity, required fields, and that all referenced skill/agent paths exist.

## Commit

Commit regularly with succinct and clear messages.

## Publishing

Only humans publish. Instruct the human to run `bb publish` to trigger a release. Requires: on `next` branch, clean tree, ahead of `master`, and at least one unreleased changelog entry.
