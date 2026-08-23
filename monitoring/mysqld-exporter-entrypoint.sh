#!/usr/bin/env sh

set -eu

cat > /tmp/.my.cnf <<EOF
[client]
host=mysql
port=3306
user=prometheus_exporter
password=${MYSQL_EXPORTER_PASSWORD}
EOF

exec /bin/mysqld_exporter --config.my-cnf=/tmp/.my.cnf
