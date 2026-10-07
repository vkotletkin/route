#!/bin/sh
# Publish sample XML into vhost "in", exchange xml.in.
# orders has priority 1, invoices has priority 2.
# A document that matches both goes only to invoices.
set -eu

publish() {
  python3 -c '
import base64, json, sys, urllib.request
body = json.dumps({
    "properties": {"content_type": "text/plain"},
    "routing_key": "xml.inbound",
    "payload": sys.argv[1],
    "payload_encoding": "string",
}).encode()
request = urllib.request.Request(
    "http://localhost:15672/api/exchanges/in/xml.in/publish",
    data=body,
    method="POST",
)
token = base64.b64encode(b"router:router").decode()
request.add_header("Authorization", "Basic " + token)
request.add_header("content-type", "application/json")
with urllib.request.urlopen(request) as response:
    print(response.read().decode())
' "$1"
}

publish '<document><order id="1"><status>new</status></order></document>'
publish '<document><invoice id="9"/></document>'
publish '<document><order id="2"/><invoice id="3"/></document>'
