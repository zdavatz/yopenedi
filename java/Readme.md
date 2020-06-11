This repository has multiple Gradle projects.

- openedi-converter - A library that parse EDIFACT D.96A and output Opentrans 2.1 XML
- openedi-exe - A command line (jar) for converting EDIFACT files
- (not started yet) openedi-email-service - A service that connects to an inbox and automatically convert EDIFACT files from attachments

To build jar file:

```
make jar
```

To run:

```
java -jar ./openedi-exe/build/libs/openedi-exe-1.0-all.jar -o testout -i ./edifile
# or
java -jar ./openedi-exe/build/libs/openedi-exe-1.0-all.jar -o testout < ./edifile
```
