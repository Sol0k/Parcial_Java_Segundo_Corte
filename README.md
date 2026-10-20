# Parcial Java Segundo Corte

MVP funcional del sistema UTS para gestión y seguimiento de trabajos de grado.

## Módulos

Autenticación, roles, banco de ideas, decisiones del comité, proyectos, equipos, asignaciones de docentes, documentos y pagos por URL, evaluaciones, prórrogas de 180 días, PQRS, calendario, usuarios e historial.

## Ejecutar

`docker compose up --build` y abrir `http://localhost:8080`.

Usuarios demo: `admin@uts.edu.co`, `comite@uts.edu.co`, `director@uts.edu.co`, `evaluador@uts.edu.co` y `estudiante@uts.edu.co`. Contraseña: `demo123`.

## Desplegar

El servicio usa `Dockerfile` y escucha en `PORT`. En Northflank crea un servicio desde este repositorio y un MySQL administrado; configura `DB_URL`, `DB_USER` y `DB_PASSWORD`.

El sistema registra documentos y soportes mediante URL; no carga archivos.