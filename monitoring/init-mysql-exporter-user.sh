#!/usr/bin/env sh

set -eu

# .env.performance의 MYSQL_EXPORTER_PASSWORD는 작은따옴표를 포함하지 않는 값으로 사용한다.
mysql --protocol=socket -u root -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'prometheus_exporter'@'%' IDENTIFIED BY '${MYSQL_EXPORTER_PASSWORD}';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'prometheus_exporter'@'%';
FLUSH PRIVILEGES;
SQL
