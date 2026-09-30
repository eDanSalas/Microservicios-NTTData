#!/bin/sh
set -eu

if [ ! -S /var/run/docker.sock ]; then
  echo "Docker socket is missing. Run with docker compose -f compose.tests.yml run --rm tests." >&2
  exit 1
fi

case "${REPORT_SET:-full}" in
  full|critical|module) ;;
  *) echo "REPORT_SET must be full, critical or module." >&2; exit 1 ;;
esac

# Export reports on success and on failure, preserving the module directories.
export_reports() {
  mkdir -p "/reports/${REPORT_SET:-full}"
  find . -type d \( -name surefire-reports -o -name failsafe-reports \) \
    -exec cp -r --parents '{}' "/reports/${REPORT_SET:-full}/" \;
}
trap export_reports EXIT

mvn -B -ntp -Dmaven.gitcommitid.skip=true "$@"

# The critical profile must never pass through skipped container tests.
case " $* " in
  *-Pcritical-regressions*)
    for suite in OrderOutboxMongoTest KitchenEventTransactionMongoTest RabbitOrderMessagingIntegrationTest; do
      report=$(find . -name "TEST-*.$suite.xml" -print)
      if [ -z "$report" ] || ! grep -Eq 'tests="[1-9][0-9]*"' "$report" \
        || ! grep -Eq 'skipped="0"' "$report"; then
        echo "Missing, empty or skipped critical integration suite: $suite" >&2
        exit 1
      fi
    done
    if find . -name 'TEST-*.xml' -exec grep -El 'skipped="[1-9][0-9]*"' '{}' \; | grep -q .; then
      echo "Critical profile contains skipped tests." >&2
      exit 1
    fi
    ;;
esac
