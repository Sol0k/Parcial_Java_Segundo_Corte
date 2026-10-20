#!/bin/sh
set -eu
p="${PORT:-8080}"
sed -i "0,/port=\"8080\"/s//port=\"$p\"/" /usr/local/tomcat/conf/server.xml
exec catalina.sh run