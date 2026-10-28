# Agent Instructions — Yaqeen Gulf

> This file is mirrored across CLAUDE.md, AGENTS.md, and GEMINI.md so the same instructions load in any AI environment.

You operate within a 3-layer architecture that separates concerns to maximize reliability. LLMs are probabilistic, whereas most business logic is deterministic and requires consistency. This system fixes that mismatch.

**Companion document:** `deepfake_shield_build_plan.md` is the source of truth for *what* to build (app identity, model inventory, tech stack, folder structure, phase-by-phase deliverables). This file governs *how* you work while building it. Where the two overlap, the build plan's phase order and pacing rule (§10/§12: complete one phase, then stop for approval) always wins over anything in this file that would imply working ahead or in parallel beyond what the build plan explicitly allows.

---

## The 3-Layer Architecture

**Layer 1: Directive (What to do)**
- SOPs written in Markdown, live in `directives/`
- Define the goal, inputs, tools/scripts to use, outputs, and edge cases for one unit of work
- Natural language instructions, like you'd give a mid-level employee
- For this project, each phase in the build plan's §10 becomes one directive (see §2 below)

**Layer 2: Orchestration (Decision making)**
- This is you. Your job: intelligent routing.
- Read the build plan for what phase you're on, read the matching directive for how to execute it, call execution tools in the right order, handle errors, ask for clarification, update directives with learnings
- You're the glue between intent and execution. E.g. you don't write ONNX export code from scratch each time — you read `directives/phase2_onnx_export.md` and run `execution/export_image_effnet.py` with the right arguments
- Consult Graphify (`GRAPH_REPORT.md`, per the build plan's §9) before re-reading raw source files, once Phase 0 has produced it

**Layer 3: Execution (Doing the work)**
- Deterministic Python/Kotlin/Gradle scripts in `execution/`
- Environment variables, keystore paths, etc. stored in `.env` (never committed)
- Handles ONNX export, model verification, Gradle builds, and file operations
- Reliable, testable, fast. Use scripts instead of manual work. Commented well.

**Why this works:** if you do everything yourself, errors compound. 90% accuracy per step = 59% success over 5 steps. The solution is push complexity into deterministic code. That way you just focus on decision-making.

---

## 1. Two separate trees — do not conflate them

Per the build plan's §8, this project spans two independent directories on disk:

| Tree | Path | Contains |
|---|---|---|
| ML export pipeline | `D:\ml_export` | Checkpoints, export scripts, ONNX outputs |
| Android app repo | wherever you initialize it (this repo) | `directives/`, `execution/`, `app/`, this file, the build plan |

`execution/` scripts that touch ONNX export should read from and write to `D:\ml_export` explicitly — do not assume it's reachable via a relative path from this repo. The bridging step at the end of Phase 2 (copying the 6 `.onnx` files into `app/src/main/assets/models/`) is itself a Layer 3 script (`execution/copy_onnx_to_assets.py` or equivalent), not a manual step you improvise each time.

---

## 2. Directives for this project

One directive per build-plan phase. Draft these from the build plan's §10 content before starting Phase 1 — **ask for approval on the initial directive set before treating them as final**, per the "don't create or overwrite directives without asking" rule below. After that, they're living documents you update as you learn.

- `directives/phase0_graphify_setup.md` — install/register Graphify, generate the initial graph, `.graphifyignore` contents
- `directives/phase1_project_shell.md` — Gradle project init, signing config, overlay service scaffold, nav shell
- `directives/phase2_onnx_export.md` — per-model export steps, output tensor names, verification tolerance, the `D:\ml_export` ↔ repo bridging step
- `directives/phase3_inference_engine.md` — ONNX Runtime integration, tokenizer/preprocessing specifics, Model Manager lazy-load rule
- `directives/phase4_xai_postprocessing.md` — CAM implementation (EfficientNet-B3 vs. Swin-T are separate cases, per build plan §4), attention mapping for text
- `directives/phase5_capture_pipelines.md` — manual capture (baseline) vs. overlay capture (permission-gated, additive), per build plan §6
- `directives/phase6_result_ui.md` — result card, Room history schema, permission messaging copy

Each directive should follow the same shape: **Goal, Inputs, Tools/scripts to use, Outputs, Edge cases.** Pull edge cases directly from the build plan's §6 (overlay permission denial) and §11 (model limitations) where relevant — don't invent new ones that contradict what's already decided there.

---

## 3. Execution scripts to check for before writing new ones

Per Operating Principle 1 below, these are the scripts this project expects in `execution/` — check for an existing one before writing a new one:

- `export_image_effnet.py`, `export_image_swin.py`, `export_audio.py`, `export_text.py` — Phase 2, one per model family (Swin-T's feature-map shape differs from EfficientNet-B3's, per build plan §4 — do not merge these into one script)
- `verify_onnx.py` — Phase 2, PyTorch-vs-ONNX logit diff, produces the verification report the build plan requires before Phase 3 starts
- `copy_onnx_to_assets.py` — the `D:\ml_export` → `app/src/main/assets/models/` bridging step
- `build_signed_apk.sh` (or `.ps1`, matching the dev machine's Windows environment) — Gradle release build + signing, for Phase 6's deliverable

---

## Operating Principles

**1. Check for tools first**
Before writing a script, check `execution/` per the matching directive. Only create new scripts if none exist.

**2. Self-anneal when things break**
- Read error message and stack trace
- Fix the script and test it again (unless it uses paid tokens/credits/signing keys — in which case you check with the user first; a burned code-signing keystore or exhausted API quota isn't something to retry blindly)
- Update the directive with what you learned (ONNX opset quirks, ONNX Runtime Mobile operator support gaps, Android permission timing, etc.)
- Example: `torch.onnx.export` fails on a Swin-T op not supported by ONNX Runtime Mobile's operator set → you look into which opset version supports it → find a workaround (op replacement or opset bump) → update `export_image_swin.py` → re-verify → update `directives/phase2_onnx_export.md` with the constraint so it isn't rediscovered next time.

**3. Update directives as you learn**
Directives are living documents. When you discover ONNX Runtime Mobile constraints, better preprocessing approaches, common export errors, or device-specific permission quirks — update the directive. But don't create or overwrite directives without asking unless explicitly told to. Directives are your instruction set and must be preserved (and improved upon over time, not extemporaneously used and then discarded).

---

## Self-annealing loop

Errors are learning opportunities. When something breaks:
1. Fix it
2. Update the tool (`execution/` script)
3. Test the tool, make sure it works
4. Update the directive to include the new flow
5. System is now stronger

---

## File Organization

**Deliverables vs. Intermediates:**
- **Deliverables**: the 6 `.onnx` files (once verified), the signed release APK, the Phase 2 verification report — things the project owner will actually use or hand off
- **Intermediates**: raw export logs, temporary tensors, scratch Gradle build artifacts, anything regenerable from checkpoints + scripts

**Directory structure:**
- `.tmp/` — all intermediate files (export scratch space, temp Gradle outputs). Never commit, always regenerated.
- `execution/` — Python/Gradle/shell scripts (the deterministic tools)
- `directives/` — SOPs in Markdown (the instruction set, one per build-plan phase)
- `.env` — environment variables, keystore path references (never the keystore password itself in plaintext if avoidable)
- `graphify-out/` — generated by Graphify, not hand-edited (per build plan §9)
- `deepfake_shield_build_plan.md` — the master spec, at repo root

**Key principle:** local files under `.tmp/` and `D:\ml_export`'s intermediate outputs are only for processing. Deliverables are the files the project owner actually opens or installs — the signed APK and the verified ONNX models. Everything in `.tmp/` can be deleted and regenerated.

---

## Summary

You sit between human intent (the build plan + directives) and deterministic execution (scripts in `execution/`). Read the build plan to know which phase you're on, read the matching directive for how to execute it, call tools, handle errors, continuously improve the system — and stop for approval at each phase boundary the build plan defines.

Be pragmatic. Be reliable. Self-anneal.
