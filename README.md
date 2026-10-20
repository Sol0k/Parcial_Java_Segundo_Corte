# Sistema UTS

Aplicación JSP/JSTL/JSPF para Tomcat 8.5 y MySQL. Sin Maven, Gradle, PHP, JavaScript propio, Servlets ni clases Java propias.

Configura el recurso JNDI `jdbc/uts`, ejecuta `database/esquema.sql` y despliega el proyecto como WAR expandido.

Limitación: sin clases Java propias no se puede implementar BCrypt/Argon2; el esquema usa SHA-256 de MySQL para no guardar texto plano. Las notificaciones son internas; no se simulan correos.