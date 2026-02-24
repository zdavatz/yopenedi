# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

yopenedi is a bidirectional converter between EDIFACT D.96A and OpenTrans 2.1 B2B document formats. It handles orders, invoices, order responses, and despatch advice documents.

## Build Commands

All commands run from `java/` directory:

```bash
# Build all JARs (CLI, email-fetcher, converter lib, server dist)
make jar

# Build individual modules
./gradlew cli:shadowJar
./gradlew email-fetcher:shadowJar
./gradlew converter:shadowJar

# Run tests
./gradlew test                    # all modules
./gradlew converter:test          # single module

# Run the Play Framework server
make run-server
ENVIRONMENT=test make run-server  # test mode

# Clean
make clean
```

The server build requires SBT (invoked via Makefile) — the converter JAR must be built first and copied to `server/lib/` before building the server.

## Architecture

```
converter (Java library)  ← core parsing & conversion logic
    ↑
    ├── cli (Java)           ← command-line batch conversion tool
    ├── email-fetcher (Java) ← IMAP inbox monitor, auto-converts attachments
    └── server (Scala/Play)  ← HTTP API with AS2 protocol support
```

**Gradle multi-project** with 4 subprojects defined in `java/settings.gradle`. The server subproject uses SBT/Play Framework separately.

### Key packages (under `com.ywesee.java.yopenedi`)

- `converter.Converter` — main entry point, auto-detects input format and converts
- `Edifact.EdifactReader` — parses EDIFACT D.96A files (uses Milyn Smooks)
- `OpenTrans.OpenTransReader` — parses OpenTrans 2.1 XML
- `common.Config` — configuration management, test mode switching
- `common.Dispatch` — routes output to email/SFTP/file based on `conf/result-dispatch.json`

### Document types

Domain models exist for: `Order`, `Invoice`, `OrderResponse`, `DespatchAdvice` — each with both EDIFACT and OpenTrans representations.

### Configuration files (`java/conf/`)

- `result-dispatch.json` / `test-result-dispatch.json` — output routing rules
- `email-credential.json` — IMAP credentials (copy from `.sample`)
- `sftpx400-credential.json` — SFTP/X.400 credentials
- `gln-override.json` — GLN (Global Location Number) overrides
- `server/conf/application.conf` — Play server config (file paths, host filtering)

### Encoding

EDIFACT files over SFTP use ISO-8859-1 encoding. This was a recent fix area (v1.1.12–1.1.13).

## Tech Stack

- **Java** (target 1.8+, CI uses OpenJDK 17) — converter, CLI, email-fetcher
- **Scala 2.13** — Play Framework server
- **Gradle 6.0.1** — Java module builds
- **SBT** — server build
- **Milyn Smooks 1.7.1** — EDIFACT parsing
- **Bouncy Castle** — PKCS#7 signatures for AS2
- **JUnit 4.12** — Java tests
- **ScalaTest+Play** — server tests
