#!/bin/sh
set -eu

usage() {
    printf '%s\n' 'Usage: ./run.sh [--once]' \
        'Build the fat JAR, start the server, and POST the autoplay request.' \
        'By default the server stays running until Ctrl+C; --once stops it after the request.' \
        'Environment: PORT (8080), REQUEST_BODY ({}), STARTUP_TIMEOUT (60), REQUEST_TIMEOUT (660).'
}

once=false
case "${1:-}" in
    '') ;;
    --once) once=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
esac
if [ "$#" -ne 0 ]; then
    usage >&2
    exit 2
fi

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$ROOT_DIR"
PORT=${PORT:-8080}
STARTUP_TIMEOUT=${STARTUP_TIMEOUT:-60}
REQUEST_TIMEOUT=${REQUEST_TIMEOUT:-660}
REQUEST_BODY=${REQUEST_BODY:-'{}'}
for number in "$PORT" "$STARTUP_TIMEOUT" "$REQUEST_TIMEOUT"; do
    case "$number" in
        ''|*[!0-9]*|0) printf '%s\n' 'Port and timeouts must be positive integers.' >&2; exit 2 ;;
    esac
done
if [ "$PORT" -gt 65535 ]; then
    printf '%s\n' 'PORT must be between 1 and 65535.' >&2
    exit 2
fi
for tool in mvn java curl; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        printf 'Required command not found: %s\n' "$tool" >&2
        exit 1
    fi
done

BASE_URL="http://127.0.0.1:$PORT"
if curl --noproxy '*' --silent --output /dev/null --connect-timeout 1 --max-time 2 "$BASE_URL/"; then
    printf 'Port %s is already serving HTTP. Choose another port, e.g. PORT=8081 ./run.sh\n' "$PORT" >&2
    exit 1
fi

LIB_DIR="$ROOT_DIR/lib"
LIB_GROUP_ID=com.bmv
MAVEN_REPO=${MAVEN_REPO:-"$HOME/.m2/repository"}
# Installs lib/<artifact>-<version>[-<classifier>].jar with its lib/<artifact>-<version>.pom unless already in MAVEN_REPO.
install_lib() {
    artifact=$1 version=$2 classifier=${3:-}
    file="$artifact-$version${classifier:+-$classifier}.jar"
    if [ -f "$MAVEN_REPO/$(printf '%s' "$LIB_GROUP_ID" | tr . /)/$artifact/$version/$file" ]; then
        return
    fi
    printf 'Installing %s into %s...\n' "$file" "$MAVEN_REPO"
    mvn -B -ntp -q org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
        -Dmaven.repo.local="$MAVEN_REPO" \
        -Dfile="$LIB_DIR/$file" \
        -DpomFile="$LIB_DIR/$artifact-$version.pom" \
        ${classifier:+-Dclassifier="$classifier"}
}
install_lib flow-foundry 1.0.0
install_lib flow-foundry 1.0.0 tests
install_lib bmv-data-redis-starter 1.0.0

printf '%s\n' 'Building the executable fat JAR...'
mvn -B -ntp -Dmaven.repo.local="$MAVEN_REPO" -pl gateway-service -am -DskipTests package
set -- "$ROOT_DIR"/gateway-service/target/gateway-service-*.jar
if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then
    printf '%s\n' 'Expected exactly one gateway-service/target/gateway-service-*.jar.' >&2
    exit 1
fi
JAR_PATH=$1
LOG_FILE="$ROOT_DIR/gateway-service/target/server-$PORT.log"
server_pid=''
cleanup() {
    if [ -n "$server_pid" ]; then
        if kill -0 "$server_pid" 2>/dev/null; then
            printf '\nStopping server (PID %s)...\n' "$server_pid"
            kill "$server_pid" 2>/dev/null || true
        fi
        wait "$server_pid" 2>/dev/null || true
    fi
}
trap cleanup 0
trap 'exit 130' INT
trap 'exit 143' TERM

printf 'Starting %s\nServer log: %s\n' "$JAR_PATH" "$LOG_FILE"
java -jar "$JAR_PATH" \
    --spring.profiles.active=local \
    --server.address=127.0.0.1 \
    --server.port="$PORT" >"$LOG_FILE" 2>&1 &
server_pid=$!

elapsed=0
until curl --noproxy '*' --fail --silent --output /dev/null \
    --connect-timeout 1 --max-time 2 "$BASE_URL/actuator/health/readiness"; do
    if ! kill -0 "$server_pid" 2>/dev/null; then
        printf '%s\n' 'Server exited during startup:' >&2
        tail -n 60 "$LOG_FILE" >&2
        exit 1
    fi
    if [ "$elapsed" -ge "$STARTUP_TIMEOUT" ]; then
        printf 'Server was not ready within %s seconds:\n' "$STARTUP_TIMEOUT" >&2
        tail -n 60 "$LOG_FILE" >&2
        exit 1
    fi
    sleep 1
    elapsed=$((elapsed + 1))
done
if ! kill -0 "$server_pid" 2>/dev/null; then
    tail -n 60 "$LOG_FILE" >&2
    exit 1
fi

printf 'POST %s/bmv/dragonsofmugloar/api/v1/autoplay\n' "$BASE_URL"
curl --noproxy '*' --fail-with-body --silent --show-error \
    --connect-timeout 5 --max-time "$REQUEST_TIMEOUT" \
    --request POST "$BASE_URL/bmv/dragonsofmugloar/api/v1/autoplay" \
    --header 'Content-Type: application/json' \
    --header 'Accept: application/json' \
    --data "$REQUEST_BODY" \
    --write-out '\n'

if [ "$once" = false ]; then
    printf '\nServer is running at %s (PID %s). Press Ctrl+C to stop.\n' "$BASE_URL" "$server_pid"
    wait "$server_pid"
fi
