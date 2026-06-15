## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When working in `sync_ad_maximo`, use the `graphify-codex` skill by default for codebase questions, architecture review, planning, and implementation work. Do not skip the graph unless the user explicitly asks not to use it or the task is specifically about correcting stale graph output.

When the user types `/graphify`, invoke the `skill` tool with `skill: "graphify"` before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

Codex setup and recovery:
- If `/graphify` stops working in Codex, reinstall from the repo root with `graphify install --project --platform codex`.
- Verify the local CLI with `graphify --version`.
- Verify the hook path with `graphify hook-check`.
- Keep `.codex/hooks.json` committed so the PreToolUse hook survives new sessions on this machine.
