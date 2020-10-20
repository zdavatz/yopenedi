This repository has multiple Gradle projects.

- openedi-converter - A library that parse EDIFACT D.96A and output Opentrans 2.1 XML
- openedi-cli - A command line (jar) for converting EDIFACT files
- openedi-email-fetcher - A tool that connects to an inbox and automatically convert EDIFACT files from attachments
- openedi-server - A server that receives HTTP POST requests of edifact

To build jar files:

```
make jar
```

To run converter:

```
# Edifact -> OpenTrans
java -jar ./cli/build/libs/cli-1.0-all.jar -o testout -i ./edifile

# OpenTrans -> Edifact
java -jar ./cli/build/libs/cli-1.0-all.jar -i ./opentrans -o ./edifact

# Send email after converting
Update the config in `java/conf/result-dispatch.json`
```

To run email-fetcher:

```
java -jar ./email-fetcher/build/libs/email-fetcher-1.0-all.jar \
	--edifact="./edifact_files" \
	--opentrans="./opentrans_files" \
	--skip-seen \
	--mark-as-seen
```

To run server:

```
make run-server
```

To configure server:
- In `server/conf/application.conf`, you can set where to save the files
  - `edifact-orders`
  - `opentrans-orders`
  
# Requirements
1. openjdk-8-jre: `apt-get install openjdk-8-jre`
2. openjdk-8-jdk: `apt-get install openjdk-8-jre`
3. Activator: `wget https://downloads.typesafe.com/typesafe-activator/1.3.9/typesafe-activator-1.3.9-minimal.zip`
4. Make: `apt install make`
5. deamontools: `daemontools` and `daemontools-run`
