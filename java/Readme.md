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

You should first setup email credentials by copying `conf/email-credential.json.sample` to `conf/email-credential.json`.

```
java -jar ./email-fetcher/build/libs/email-fetcher-1.0-all.jar \
	--edifact="./edifact_files" \
	--opentrans="./opentrans_files" \
	--skip-seen \
	--mark-as-seen
```

You can enable test mode by `--test`.

To run server:

```
make run-server
```

You can enable test mode via environment variable:
```
ENVIRONMENT=test make run-server
```

To configure server:
- In `server/conf/application.conf`, you can set where to save the files
  - `edifact-orders`
  - `opentrans-orders`

## Test mode

When test mode is enabled:
- it reads from `conf/test-email-credential.json`.
  - You should create such file by copying it from `conf/email-credential.json.sample`
  - If this file is not found, it reads from `conf/email-credential.json`
- it reads from `conf/test-result-dispatch.json`.
  - If this file is not found, it reads from `conf/result-dispatch.json`
- it exports OpenTrans files with test environment message.

# Requirements
1. openjdk-8-jre: `apt install openjdk-8-jre`
2. openjdk-8-jdk: `apt install openjdk-8-jdk`
3. Activator: `wget https://downloads.typesafe.com/typesafe-activator/1.3.9/typesafe-activator-1.3.9-minimal.zip`
4. Make: `apt install make`
5. deamontools: `apt install daemontools` and `apt install daemontools-run`
# Setup
1. Link Daemontools: `sudo ln -s /var/www/yopenedi.ch/svc/ /etc/service/yopenedi.ch`
2. Setup up log `mkdir /var/www/yopenedi.ch/svc/`
3. make `run` executable `chmod +x run` (also for `log`)
4. make sure to restart `systemctl restart daemontools` so the supervise process starts the logging.
5. Enable Proxy for Apache2: `sudo a2enmod proxy`, `sudo a2enmod proxy_http` and `sudo a2enmod ssl`
