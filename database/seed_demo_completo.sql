USE trabajos_grado_uts;

INSERT INTO usuario(documento,nombre,correo,password_sha256,rol) VALUES
('1000000001','Estudiante Demo','estudiante.demo@uts.edu.co',SHA2('Demo1234!',256),'ESTUDIANTE'),
('1000000002','Director Demo','director.demo@uts.edu.co',SHA2('Demo1234!',256),'DIRECTOR'),
('1000000003','Evaluador Demo','evaluador.demo@uts.edu.co',SHA2('Demo1234!',256),'EVALUADOR'),
('1000000004','Comité Demo','comite.demo@uts.edu.co',SHA2('Demo1234!',256),'COMITE')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre),activo=TRUE;

INSERT IGNORE INTO estudiante(usuario_id,programa,nivel,creditos_aprobados)
SELECT id,'Ingeniería de Sistemas','Profesional',85 FROM usuario WHERE correo='estudiante.demo@uts.edu.co';

INSERT IGNORE INTO docente(usuario_id,tipo,carga_maxima)
SELECT id,'AMBOS',5 FROM usuario WHERE correo='director.demo@uts.edu.co';
INSERT IGNORE INTO docente(usuario_id,tipo,carga_maxima)
SELECT id,'EVALUADOR',5 FROM usuario WHERE correo='evaluador.demo@uts.edu.co';

INSERT IGNORE INTO idea(titulo,descripcion,nivel,modalidad,estado,creado_por)
SELECT 'Sistema de seguimiento de trabajos de grado','Plataforma para controlar etapas, documentos, pagos y decisiones.','Profesional','Proyecto','PENDIENTE',id FROM usuario WHERE correo='estudiante.demo@uts.edu.co';
INSERT IGNORE INTO idea(titulo,descripcion,nivel,modalidad,estado,creado_por)
SELECT 'Analítica académica para proyectos','Panel de indicadores para detectar vencimientos y cargas docentes.','Profesional','Proyecto','APROBADA',id FROM usuario WHERE correo='director.demo@uts.edu.co';

INSERT IGNORE INTO proyecto(codigo,titulo,modalidad,estado,etapa,creado_por)
SELECT 'SIS-2026.1-01','Sistema demo de trabajos de grado','Proyecto','PROPUESTA',1,id FROM usuario WHERE correo='estudiante.demo@uts.edu.co';

INSERT IGNORE INTO integrante(proyecto_id,usuario_id)
SELECT p.id,u.id FROM proyecto p JOIN usuario u ON u.correo='estudiante.demo@uts.edu.co' WHERE p.codigo='SIS-2026.1-01';

INSERT IGNORE INTO asignacion(proyecto_id,docente_id,tipo,estado)
SELECT p.id,d.usuario_id,'DIRECTOR','ACTIVA' FROM proyecto p JOIN docente d JOIN usuario u ON u.id=d.usuario_id WHERE p.codigo='SIS-2026.1-01' AND u.correo='director.demo@uts.edu.co';
INSERT IGNORE INTO asignacion(proyecto_id,docente_id,tipo,estado)
SELECT p.id,d.usuario_id,'EVALUADOR','ACTIVA' FROM proyecto p JOIN docente d JOIN usuario u ON u.id=d.usuario_id WHERE p.codigo='SIS-2026.1-01' AND u.correo='evaluador.demo@uts.edu.co';

INSERT IGNORE INTO documento(proyecto_id,tipo,url,estado,observaciones)
SELECT id,'FDC-124','https://example.com/demo-fdc-124','PENDIENTE','Pendiente de revisión del Director' FROM proyecto WHERE codigo='SIS-2026.1-01';
INSERT IGNORE INTO documento(proyecto_id,tipo,url,estado,observaciones)
SELECT id,'FDC-125','https://example.com/demo-fdc-125','PENDIENTE','Documento demo' FROM proyecto WHERE codigo='SIS-2026.1-01';

INSERT IGNORE INTO pago(proyecto_id,tipo,url,estado,observaciones)
SELECT id,'MATRÍCULA','https://example.com/demo-pago','PENDIENTE','Pendiente de verificación del Comité' FROM proyecto WHERE codigo='SIS-2026.1-01';

INSERT IGNORE INTO prorroga(proyecto_id,carta_url,pago_url,estado,observaciones)
SELECT id,'https://example.com/demo-carta','https://example.com/demo-pago-prorroga','PENDIENTE','Solicitud demo' FROM proyecto WHERE codigo='SIS-2026.1-01';

INSERT IGNORE INTO evaluacion(proyecto_id,evaluador_id,tipo,concepto,observaciones)
SELECT p.id,d.usuario_id,'PROPUESTA','APROBADO_CON_CORRECCIONES','Agregar alcance y cronograma.' FROM proyecto p JOIN docente d JOIN usuario u ON u.id=d.usuario_id WHERE p.codigo='SIS-2026.1-01' AND u.correo='evaluador.demo@uts.edu.co';

INSERT IGNORE INTO pqrs(usuario_id,tipo,asunto,descripcion,estado)
SELECT id,'PETICION','Consulta sobre fecha de entrega','Solicito información sobre el próximo vencimiento.','ABIERTA' FROM usuario WHERE correo='estudiante.demo@uts.edu.co';

INSERT IGNORE INTO historial(usuario_id,entidad,entidad_id,accion,estado_nuevo,observaciones)
SELECT u.id,'PROYECTO',p.id,'CREACION','PROPUESTA','Proyecto demo cargado por seed.' FROM usuario u JOIN proyecto p WHERE u.correo='estudiante.demo@uts.edu.co' AND p.codigo='SIS-2026.1-01';

INSERT IGNORE INTO notificacion(usuario_id,mensaje)
SELECT id,'Tienes un proyecto demo con documentos y pagos pendientes.' FROM usuario WHERE correo='estudiante.demo@uts.edu.co';

INSERT IGNORE INTO calendario(nombre,apertura,cierre,activo) VALUES('Periodo demo 2026-1','2026-01-15','2026-06-30',TRUE);