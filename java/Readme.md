This repository has multiple Gradle projects.

- openedi-converter - A library that parse EDIFACT D.96A and output Opentrans 2.1 XML
- openedi-exe - A command line (jar) for converting EDIFACT files
- (not started yet) openedi-email-service - A service that connects to an inbox and automatically convert EDIFACT files from attachments

To run `openedi-exe`:

```
make run OUT='./testout' < edifact file
```
