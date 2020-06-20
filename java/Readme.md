This repository has multiple Gradle projects.

- openedi-converter - A library that parse EDIFACT D.96A and output Opentrans 2.1 XML
- openedi-exe - A command line (jar) for converting EDIFACT files
- openedi-email-fetcher - A tool that connects to an inbox and automatically convert EDIFACT files from attachments

To build jar files:

```
make jar
```

To run converter:

```
java -jar ./cli/build/libs/cli-1.0-all.jar -o testout -i ./edifile
```

To run email-fetcher:

```
java -jar ./email-fetcher/build/libs/email-fetcher-1.0-all.jar \
	--edifact="./edifact_files" \
	--opentrans="./opentrans_files" \
	--mail-host="imap.gmail.com" \
	--mail-port="993" \
	--mail-secure \
	--mail-username="xxxxxx@gmail.com" \
	--mail-password="<your password>" \
	--skip-seen \
	--mark-as-seen
```
