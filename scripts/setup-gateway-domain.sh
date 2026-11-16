#!/bin/sh
set -eu

asadmin=${ASADMIN:-asadmin}
if ! command -v "$asadmin" >/dev/null 2>&1; then
    asadmin=/opt/homebrew/opt/glassfish/libexec/bin/asadmin
fi

domain=api-gateway-domain
domains=$("$asadmin" --terse=true list-domains)
if printf '%s\n' "$domains" | awk -v name="$domain" '$1 == name { found = 1 } END { exit !found }'; then
    printf '%s\n' "$domain ist bereits eingerichtet."
else
    "$asadmin" --user admin create-domain --adminport 4848 --instanceport 8080 "$domain"
fi
