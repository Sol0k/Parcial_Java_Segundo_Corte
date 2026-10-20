CREATE TABLE IF NOT EXISTS usuario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, documento VARCHAR(30) UNIQUE NOT NULL,
 nombre VARCHAR(120) NOT NULL, correo VARCHAR(150) UNIQUE NOT NULL,
 contrasena VARCHAR(255) NOT NULL, rol VARCHAR(20) NOT NULL,
 programa VARCHAR(120), nivel INT DEFAULT 1, porcentaje_creditos DECIMAL(5,2) DEFAULT 0,
 activo BOOLEAN DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS idea (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, titulo VARCHAR(180) NOT NULL,
 descripcion TEXT NOT NULL, nivel INT NOT NULL, estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
 observaciones TEXT
);
CREATE TABLE IF NOT EXISTS proyecto (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, codigo VARCHAR(30) UNIQUE NOT NULL,
 idea_id BIGINT, modalidad VARCHAR(80), estado VARCHAR(40) NOT NULL DEFAULT 'PROPUESTA',
 fecha_aprobacion DATE, FOREIGN KEY (idea_id) REFERENCES idea(id)
);
CREATE TABLE IF NOT EXISTS documento (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, proyecto_id BIGINT NOT NULL,
 tipo VARCHAR(40) NOT NULL, url VARCHAR(500) NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE', fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (proyecto_id) REFERENCES proyecto(id)
);
CREATE TABLE IF NOT EXISTS pago (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, proyecto_id BIGINT NOT NULL,
 estudiante_id BIGINT NOT NULL, tipo VARCHAR(40) NOT NULL, url_soporte VARCHAR(500) NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE', FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
 FOREIGN KEY (estudiante_id) REFERENCES usuario(id)
);
CREATE TABLE IF NOT EXISTS evaluacion (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, proyecto_id BIGINT NOT NULL,
 docente_id BIGINT NOT NULL, fase VARCHAR(30) NOT NULL, concepto VARCHAR(40) NOT NULL,
 observaciones TEXT, fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS pqrs (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, usuario_id BIGINT NOT NULL,
 asunto VARCHAR(180) NOT NULL, descripcion TEXT NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'ABIERTA', respuesta TEXT,
 fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
INSERT IGNORE INTO usuario(documento,nombre,correo,contrasena,rol,programa,nivel,porcentaje_creditos)
VALUES ('1000','Administrador','admin@uts.edu.co','demo123','ADMIN',NULL,1,100),
('1001','Comité UTS','comite@uts.edu.co','demo123','COMITE',NULL,1,100),
('1002','Director Demo','director@uts.edu.co','demo123','DIRECTOR',NULL,1,100),
('1003','Evaluador Demo','evaluador@uts.edu.co','demo123','EVALUADOR',NULL,1,100),
('1004','Estudiante Demo','estudiante@uts.edu.co','demo123','ESTUDIANTE','Programación en Java',1,80);
INSERT IGNORE INTO idea(id,titulo,descripcion,nivel,estado)
VALUES (1,'Sistema de gestión de trabajos de grado',
'Plataforma web para centralizar el ciclo de vida del proyecto.',1,'DISPONIBLE');