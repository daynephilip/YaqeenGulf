# Directive: Phase 0 — Graphify Setup

## Goal
Give yourself a queryable map of this codebase so you don't burn context re-reading the same files across phases. This must succeed (or be explicitly flagged as failed with a fallback noted) before any Phase 1 work begins.

## Inputs
- None required — this runs against a fresh or near-empty repo.

## Tools / scripts to use
1. `uv tool install graphifyy` (preferred) or `pipx install graphifyy` if `uv` isn't available.
2. `graphify install` — registers the skill with the AI assistant running this build.
3. Inside the assistant: `/graphify .`
4. Create `.graphifyignore` at the project root with at minimum:
   ```
   *.onnx
   *.pt
   *.apk
   .gradle/
   build/
   ```

## Outputs
- `graphify-out/graph.html` — browsable graph
- `graphify-out/GRAPH_REPORT.md` — key concepts, connections, suggested questions
- `graphify-out/graph.json` — full queryable graph
- `.graphifyignore` at repo root

## Edge cases
- **`uv`/`pipx` not installed:** fall back to `pip install graphifyy --user` (avoid `--break-system-packages` unless nothing else works, and confirm with the user first if it comes to that).
- **`graphify` command not found after install:** check PATH before assuming failure — installers sometimes place the binary in a user-local bin directory not yet on PATH in the current shell session.
- **`/graphify .` produces an empty or near-empty graph:** expected if run before Phase 1 creates any real source files. Re-run once the Phase 1 project shell exists rather than treating a thin graph as a broken one.
- **Any step fails and you can't resolve it in one or two tries:** do not fabricate a graph or proceed as if Graphify is working. Fall back to normal file reading for this session, flag the failure to the user, and retry Phase 0 later rather than silently skipping it.
- **Do not confuse `graphifyy` (double-y, the correct package) with any other similarly-named package** — verify the install actually pulled from the intended source if anything looks off.
