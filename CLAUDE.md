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

### Timeouts and hang protection

Added in v1.1.16 after the production server was found wedging itself: senders
reported `SocketTimeoutException: Read timed out` waiting for their AS2 MDN.

Every outbound network call (HTTP POST, SMTP, IMAP, SFTP) is bounded by
`common.Timeouts`. Defaults are in seconds and can be overridden per-process with
system properties, no rebuild needed:

```bash
-Dyopenedi.connectTimeout=30    -Dyopenedi.readTimeout=120
-Dyopenedi.writeTimeout=120     -Dyopenedi.http.readTimeout=120
-Dyopenedi.maxRuntime=1800      # email-fetcher watchdog, 0 disables
```

Three layers keep a stuck partner from taking the process down:

1. **Per-call timeouts** — nothing waits forever. Previously all of these defaulted
   to "wait indefinitely".
2. **`contexts.blocking-io-dispatcher`** (server) — conversion and result dispatch
   run there, never on Play's default dispatcher, so a slow partner cannot starve
   the threads that answer requests. Since v1.1.18 the AS2 MDN is returned as soon
   as the order is stored, *before* it is even parsed: Smooks takes 12-14s on a
   large order, and senders (compacer) hit `Read timed out` and resent. A
   byte-identical copy of an order received in the last 7 days is stored but not
   forwarded again. Conversion errors therefore no longer reach the sender as a
   400; they are only in the log.
3. **`common.Watchdog`** (email-fetcher) — dumps all thread stacks and halts with
   exit code 3 if a run exceeds `maxRuntime`, so a hang self-documents in the log.

`scripts/yopenedi-healthcheck.sh` covers the supervisor gap: daemontools restarts a
service that exits, but never one that is merely wedged. Run it from `/etc/crontab`
as root (that file needs a user column; user crontabs do not).

Production is deployed with `/usr/local/bin/update_yopenedi` on the server, which
pulls the release artifacts -- its version number must be bumped for each release.
Never build on the server: pushing a tag makes `.github/workflows/release.yml` build
and attach the three artifacts. Build and test locally first.

### Production server pitfalls

- The Play server runs in `/var/www/yopenedi.ch`, but its start script sets
  `user.dir` to the dist folder. `java.io.File` resolves the relative
  `./edifact_orders` against the process cwd (where the files are), `java.nio`
  (`Files.*`, `File.toPath()`) against `user.dir` (where they are not), and
  `getAbsolutePath()` in the log shows the wrong folder. Use `java.io` streams in the
  server; 1.1.18 used `Files.readAllBytes` and dropped an order with
  `NoSuchFileException` (fixed in 1.1.19).
- Logs are in `/etc/service/prod.yopenedi.ch/log/main/current` (pipe through
  `tai64nlocal`), not `logs/application.log`, which stays empty.
- `AllowedHostsFilter` only accepts `localhost:9000`, `.yopenedi.ch` and the IP:
  server tests need `HOST -> "localhost:9000"`.
- Smooks 1.7.1 cannot *read* EDIFACT on JDK 21 (`java.lang.Compiler` is gone); run
  tests with JDK 17, like CI and production.

### Invoices: EN 16931

FEGIME requires EN 16931 data in EDI-INVOIC from November 2026 (rejects from January
2027); see the table in README.md. Added in 1.1.17 in `Edifact.Invoice` and
`converter.Converter.invoiceToEdifact`, covered by `InvoiceEN16931Test` with a
synthetic invoice (never commit partner data as fixtures). The payment term mapping
used to write the invoice date as due date and 3% discount as `PCD+12:30`.

### Encoding

EDIFACT files over SFTP use ISO-8859-1 encoding. This was a recent fix area (v1.1.12–1.1.13).

### VRG AS2 Endpoints

VRG AS2 endpoints were migrated from `eddy.vrg.de` to `edi-p-as2.vrg.de:443` (production) and `edi-t-as2.vrg.de:443` (test) in v1.1.14–1.1.15.

## Tech Stack

- **Java** (target 1.8+, CI uses OpenJDK 17) — converter, CLI, email-fetcher
- **Scala 2.13** — Play Framework server
- **Gradle 6.0.1** — Java module builds
- **SBT** — server build
- **Milyn Smooks 1.7.1** — EDIFACT parsing
- **Bouncy Castle** — PKCS#7 signatures for AS2
- **JUnit 4.12** — Java tests
- **ScalaTest+Play** — server tests
