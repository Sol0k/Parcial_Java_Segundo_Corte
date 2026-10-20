USE trabajos_grado_uts;

INSERT INTO usuario(documento,nombre,correo,password_sha256,rol)
VALUES
('1000000001','Estudiante Demo','estudiante.demo@uts.edu.co',SHA2('Demo1234!',256),'ESTUDIANTE'),
('1000000002','Director Demo','director.demo@uts.edu.co',SHA2('Demo1234!',256),'DIRECTOR'),
('1000000003','Evaluador Demo','evaluador.demo@uts.edu.co',SHA2('Demo1234!',256),'EVALUADOR'),
('1000000004','Comité Demo','comite.demo@uts.edu.co',SHA2('Demo1234!',256),'COMITE')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre),activo=TRUE;

INSERT INTO estudiante(usuario_id,programa,nivel,creditos_aprobados)
SELECT id,'Ingeniería de Sistemas','Profesional',85.00 FROM usuario WHERE correo='estudiante.demo@uts.edu.co'
AND NOT EXISTS (SELECT 1 FROM estudiante e JOIN usuario u ON u.id=e.usuario_id WHERE u.correo='estudiante.demo@uts.edu.co');

INSERT INTO docente(usuario_id,tipo,carga_maxima)
SELECT id,'AMBOS',5 FROM usuario WHERE correo='director.demo@uts.edu.co'
AND NOT EXISTS (SELECT 1 FROM docente d WHERE d.usuario_id=(SELECT id FROM usuario WHERE correo='director.demo@uts.edu.co'));

INSERT INTO docente(usuario_id,tipo,carga_maxima)
SELECT id,'EVALUADOR',5 FROM usuario WHERE correo='evaluador.demo@uts.edu.co'
AND NOT EXISTS (SELECT 1 FROM docente d WHERE d.usuario_id=(SELECT id FROM usuario WHERE correo='evaluador.demo@uts.edu.co'));

INSERT INTO idea(titulo,descripcion,nivel,modalidad,estado,creado_por)
SELECT 'Sistema de seguimiento de trabajos de grado','Idea de prueba para validar el banco de ideas.','Profesional','Proyecto','PENDIENTE',id
FROM usuario WHERE correo='estudiante.demo@uts.edu.co'
AND NOT EXISTS (SELECT 1 FROM idea WHERE titulo='Sistema de seguimiento de trabajos de grado');

INSERT INTO notificacion(usuario_id,mensaje)
SELECT id,'Cuenta demo creada correctamente.' FROM usuario
WHERE correo IN ('estudiante.demo@uts.edu.co','director.demo@uts.edu.co','evaluador.demo@uts.edu.co','comite.demo@uts.edu.co')
AND NOT EXISTS (SELECT 1 FROM notificacion n WHERE n.usuario_id=usuario.id AND n.mensaje='Cuenta demo creada correctamente.');

-- Credenciales demo: todos usan Demo1234!
-- admin@uts.edu.co / CambiarEstaClave
