#!/bin/sh
set -eu

glassfish_home=${GLASSFISH_HOME:-/opt/homebrew/opt/glassfish/libexec/glassfish}
if [ -x "$glassfish_home/glassfish/bin/asadmin" ]; then
    glassfish_home="$glassfish_home/glassfish"
fi
asadmin="$glassfish_home/bin/asadmin"
domains=${AS_DEF_DOMAINS_PATH:-"$glassfish_home/domains"}
case "${1:-}" in
    user-service) portbase=10000 ;;
    order-service) portbase=11000 ;;
    shipping-service) portbase=12000 ;;
    *) echo "Aufruf: sh $0 {user-service|order-service|shipping-service}" >&2; exit 2 ;;
esac
domain="$1-domain"
config="$domains/$domain/config/domain.xml"

if [ ! -d "$domains/$domain" ]; then
    "$asadmin" --user admin create-domain --domaindir "$domains" --portbase "$portbase" "$domain"
fi

# IntelliJ's GlassFish integration expects a numeric JDWP address.
python3 - "$config" "$((portbase + 9))" <<'PY'
import pathlib
import sys
import xml.etree.ElementTree as ET

path = pathlib.Path(sys.argv[1])
content = path.read_bytes()
server = ET.fromstring(content).find(".//config[@name='server-config']")
options = server.find("java-config").attrib["debug-options"]
port = sys.argv[2]
wildcard = f"address=*:{port}"
local = f"address={port}"
if wildcard in options.split(","):
    before = options.encode()
    path.write_bytes(content.replace(before, before.replace(wildcard.encode(), local.encode()), 1))
elif local not in options.split(","):
    sys.exit("Unerwarteter Debug-Port in " + str(path))
PY
printf 'Domain eingerichtet: %s\nHTTP: %s | Admin: %s | Debug: %s\n' "$domains/$domain" "$((portbase + 80))" "$((portbase + 48))" "$((portbase + 9))"
printf 'Starte die Domain jetzt in IntelliJ mit Debug. Serverlog nach dem Start: %s/logs/server.log\n' "$domains/$domain"
