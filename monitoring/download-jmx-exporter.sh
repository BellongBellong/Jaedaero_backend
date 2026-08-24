#!/usr/bin/env sh

set -eu

version="1.6.0"
destination="$(dirname "$0")/jmx_prometheus_javaagent.jar"
url="https://github.com/prometheus/jmx_exporter/releases/download/${version}/jmx_prometheus_javaagent-${version}.jar"

curl --fail --location --retry 3 --output "$destination" "$url"
echo "Downloaded JMX exporter ${version} to ${destination}"
