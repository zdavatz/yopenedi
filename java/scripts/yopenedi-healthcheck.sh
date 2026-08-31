#!/bin/sh
# Liveness check for the yopenedi Play server.
#
# The supervisor restarts a service that *exits*, but never one that is merely
# wedged: a JVM whose request threads are all blocked stays alive forever and is
# never restarted. This closes that gap.
#
# Any HTTP response counts as healthy -- even a 400 from the host filter proves the
# request threads are still being served. Only a timeout or a refused connection
# means the process is stuck.
#
# Install in /etc/crontab (note the user column, which user crontabs do not have):
#   */2 * * * * root /home/zdavatz/software/yopenedi/java/scripts/yopenedi-healthcheck.sh

SERVICE_DIR="${SERVICE_DIR:-/etc/service/prod.yopenedi.ch}"
URL="${URL:-http://127.0.0.1:9000/}"
HOST_HEADER="${HOST_HEADER:-prod.yopenedi.ch}"
TIMEOUT="${TIMEOUT:-20}"
# Only restart after this many consecutive failures, so one slow moment is not
# enough to bounce the service mid-transfer.
FAILURES_BEFORE_RESTART="${FAILURES_BEFORE_RESTART:-3}"
STATE_FILE="${STATE_FILE:-/tmp/yopenedi-healthcheck.fails}"

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') yopenedi-healthcheck: $*"
}

# Works under both daemontools (svc/svstat) and runit (sv).
service_pid() {
    if command -v svstat >/dev/null 2>&1; then
        svstat "$SERVICE_DIR" 2>/dev/null | sed -n 's/.*(pid \([0-9]*\)).*/\1/p'
    elif command -v sv >/dev/null 2>&1; then
        sv status "$SERVICE_DIR" 2>/dev/null | sed -n 's/.*(pid \([0-9]*\)).*/\1/p'
    fi
}

restart_service() {
    if command -v svc >/dev/null 2>&1; then
        svc -t "$SERVICE_DIR"
    elif command -v sv >/dev/null 2>&1; then
        sv restart "$SERVICE_DIR"
    else
        log "neither svc nor sv found, cannot restart $SERVICE_DIR"
        return 1
    fi
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
pid=$(service_pid)
if [ -n "$pid" ]; then
    log "sending SIGQUIT to pid $pid for a thread dump"
    kill -3 "$pid" 2>/dev/null
    sleep 2
fi

log "restarting $SERVICE_DIR"
restart_service
rm -f "$STATE_FILE"
