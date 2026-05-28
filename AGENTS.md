# AGENTS.md

Documentation for AI agents and developers.

## Language

Respond in the same language as the user's question. Support Russian and English only. If the user writes in Russian, respond in Russian. If the user writes in English, respond in English. For any other language, respond in English.

## Current Information

Always use information from project documentation — [coding_style.md](./docs/coding_style.md), [architecture.md](./docs/architecture.md), [development.md](./docs/development.md), [libraries.md](./docs/libraries.md), `deps.edn`, source code. Do not rely on external knowledge about libraries, versions, or code structure.

## Freshness check for libraries

For any question about libraries, frameworks, APIs, CLI tools, configuration options, versions, or examples — verify current information using official sources before answering. Prefer official documentation, GitHub repositories, changelogs, release notes, and package registries. If current information was not checked — explicitly say so.

## Code Standards

See [coding_style.md](./docs/coding_style.md) for code style guidelines.

Do not add trivial one-line comments to function names (e.g. `;; Process data` right above `(defn process-data ...)`). Only add comments that explain non-obvious intent, constraints, or tradeoffs.

## Generated Code

When generating new functions or modules, always write corresponding tests. Keep existing tests up to date when modifying generated or existing code — update tests to reflect changed behavior, add new tests for new functionality, and remove obsolete tests.

## Main Libraries

See [libraries.md](./docs/libraries.md) for the list of libraries.

## Project Structure

- `meteomax.*` - application code
- `meteomax.app.*` - business logic
- `meteomax.data.*` - data layer
- `meteomax.metrics.*` - metrics and export
- `meteomax.lib.*` - common utilities

## After Code Edits

After making changes to code, run:

1. **Lint**:
   ```bash
   clj-kondo --lint src
   ```

2. **Tests** (if any):
   ```bash
   clj -M:test
   ```
