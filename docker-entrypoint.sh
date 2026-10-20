#!/bin/sh
set -eu
: "${MYSQLHOST:?MYSQLHOST is required}"
: "${MYSQLPORT:?MYSQLPORT is required}"
: "${MYSQLDATABASE:?MYSQLDATABASE is required}"
: "${MYSQLUSER:?MYSQLUSER is required}"
: "${MYSQLPASSWORD:?MYSQLPASSWORD is required}"
mkdir -p /usr/local/tomcat/conf/Catalina/localhost
cat > /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml <<EOF
<Context>
  <Resource name="jdbc/uts" auth="Container" type="javax.sql.DataSource"
    maxTotal="20" maxIdle="5" maxWaitMillis="10000"
    username="${MYSQLUSER}" password="${MYSQLPASSWORD}"
    driverClassName="com.mysql.cj.jdbc.Driver"
    url="jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=false&amp;serverTimezone=UTC&amp;allowPublicKeyRetrieval=true"/>
</Context>
EOF
exec catalina.sh run
