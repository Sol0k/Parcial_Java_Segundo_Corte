#!/bin/sh
set -eu
port="${PORT:-8080}"
# Render envía las comprobaciones al puerto PORT. El shutdown port no debe aceptar tráfico HTTP.
sed -i "s/port=\"8005\"/port=\"-1\"/" /usr/local/tomcat/conf/server.xml
sed -i "0,/port=\"8080\"/s//port=\"$port\"/" /usr/local/tomcat/conf/server.xml
exec catalina.sh run
