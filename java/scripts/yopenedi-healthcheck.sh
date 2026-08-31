#!/bin/sh
# Liveness check for the yopenedi Play server.
#
# runit only restarts a service that *exits*. A JVM whose request threads are all
# blocked stays alive forever and is never restarted, which is how the server used
# to go silent until someone noticed. This closes that gap.
#
# Any HTTP response counts as healthy -- even a 400 from the host filter proves the
# request threads are still being served. Only a timeout or a refused connection
# means the process is wedged.
#
# Install as a cron entry, e.g. every 2 minutes:
#   */2 * * * * /home/zdavatz/software/yopenedi/java/scripts/yopenedi-healthcheck.sh

SERVICE="${SERVICE:-prod.yopenedi.ch}"
URL="${URL:-http://127.0.0.1:9000/}"
HOST_HEADER="${HOST_HEADER:-prod.yopenedi.ch}"
TIMEOUT="${TIMEOUT:-20}"
# Only restart after this many consecutive failures, so one slow moment is not
# enough to bounce the service mid-transfer.
FAILURES_BEFORE_RESTART="${FAILURES_BEFORE_RESTART:-3}"
STATE_FILE="${STATE_FILE:-/tmp/yopenedi-healthcheck.$SERVICE.fails}"

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') yopenedi-healthcheck: $*"
}

status=$(curl --silent --output /dev/null --write-out '%{http_code}' \
    --max-time "$TIMEOUT" --header "Host: $HOST_HEADER" "$URL" 2>/dev/null)

if [ -n "$status" ] && [ "$status" != "000" ]; then
    rm -f "$STATE_FILE"
    exit 0
fi

failures=$(cat "$STATE_FILE" 2>/dev/null || echo 0)
failures=$((failures + 1))
echo "$failures" > "$STATE_FILE"
log "no response from $URL within ${TIMEOUT}s (failure $failures/$FAILURES_BEFORE_RESTART)"

if [ "$failures" -lt "$FAILURES_BEFORE_RESTART" ]; then
    exit 1
fi

# Ask the JVM for a thread dump first, so the log says what it was stuck on.
pid=$(sv status "$SERVICE" 2>/dev/null | sed -n 's/.*(pid \([0-9]*\)).*/\1/p')
if [ -n "$pid" ]; then
    log "sending SIGQUIT to pid $pid for a thread dump"
    kill -3 "$pid" 2>/dev/null
    sleep 2
fi

log "restarting $SERVICE"
sv restart "$SERVICE"
rm -f "$STATE_FILE"
