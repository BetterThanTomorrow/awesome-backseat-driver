# Cursor JSON Schemas

Pinned copies of Cursor plugin schemas from [cursor/plugins](https://github.com/cursor/plugins).

| File | Source |
|------|--------|
| `plugin.schema.json` | `schemas/plugin.schema.json` |
| `marketplace.schema.json` | `schemas/marketplace.schema.json` |

**Source commit:** `c8402bc8e3673b973719fe3acc2c4837fef34f86`

## Validator

Strict JSON Schema validation uses **Node + ajv** (`package.json`, `validate.mjs`). Babashka scripts invoke it via `scripts/cursor_schema.clj`.

Install once:

```sh
npm install --prefix schemas/cursor
```

Smoke test fixtures:

```sh
node schemas/cursor/validate.mjs -s schemas/cursor/plugin.schema.json -d schemas/cursor/fixtures/valid-plugin.json
bb validate-cursor-schema
```

The pinned `marketplace.schema.json` allows only `name`, `source`, and `description` on each `plugins[]` entry (`additionalProperties: false`). Copilot marketplace entries include `:version`; the Cursor generator strips it.
