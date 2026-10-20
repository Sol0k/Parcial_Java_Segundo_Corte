# Plan de integración por módulos

## Fase 0 — Entorno desplegable (completada en este commit)
- Dockerfile basado en Tomcat 8.5 + JDK 8.
- docker-compose con aplicación y MySQL 8.
- Persistencia de MySQL en volumen.
- Scripts SQL montados como inicialización.
- Validar: docker compose build && docker compose up.

## Fase 1 — Núcleo y seguridad (en curso)
- JNDI `jdbc/uts`.
- Login, logout y sesión.
- Guardas de sesión por servidor.
- Roles: estudiante, director, evaluador, comité y administrador.
- Validar credenciales, cuenta activa y redirección por rol.

## Fase 2 — Estudiante
1. Perfil académico y validación del 75 % de créditos.
2. Banco de ideas con búsqueda y filtros.
3. Propuesta y código de proyecto.
4. Integrantes y bloqueo después del inicio.
5. Documentos por URL.
6. Pagos y observaciones.
7. Prórrogas con límite de dos.
8. PQRS e historial.

## Fase 3 — Director
1. Proyectos asignados.
2. Revisión FDC-124/FDC-125.
3. Observaciones obligatorias al rechazar.
4. Dos seguimientos mínimos.
5. Notificaciones internas.

## Fase 4 — Evaluador
1. Asignaciones y plazos.
2. Evaluación de propuesta.
3. Evaluación de informe final.
4. Tercer evaluador por decisión del comité.

## Fase 5 — Comité
1. Aprobación/rechazo de ideas.
2. Asignaciones con carga máxima.
3. Proyectos, pagos y prórrogas.
4. Calendario y vencimientos.
5. PQRS, reportes e historial.

## Fase 6 — Administrador y auditoría
1. Alta y desactivación lógica de usuarios.
2. Docentes y roles.
3. Parámetros institucionales.
4. Historial administrativo y trazabilidad.

## Criterio de avance
Cada fase debe dejar consultas parametrizadas, autorización por propiedad/rol, historial de decisiones y una prueba manual reproducible. No se declara terminada una fase si solo tiene pantallas.
