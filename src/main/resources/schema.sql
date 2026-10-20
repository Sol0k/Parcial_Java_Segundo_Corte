CREATE TABLE IF NOT EXISTS usuario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, documento VARCHAR(30) UNIQUE NOT NULL,
 nombre VARCHAR(120) NOT NULL, correo VARCHAR(150) UNIQUE NOT NULL,
 contrasena VARCHAR(255) NOT NULL, rol VARCHAR(20) NOT NULL,
 programa VARCHAR(120), nivel INT DEFAULT 1, porcentaje_creditos DECIMAL(5,2) DEFAULT 0,
 especialidad VARCHAR(120), carga_maxima INT DEFAULT 5, activo BOOLEAN DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS idea (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, titulo VARCHAR(180) NOT NULL,
 descripcion TEXT NOT NULL, nivel INT NOT NULL, estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
 observaciones TEXT, creador_id BIGINT, leida_por_comite BOOLEAN DEFAULT FALSE,
 fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS idea_estudiante (
 idea_id BIGINT NOT NULL, estudiante_id BIGINT NOT NULL, fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(idea_id,estudiante_id), FOREIGN KEY(idea_id) REFERENCES idea(id), FOREIGN KEY(estudiante_id) REFERENCES usuario(id)
);
CREATE TABLE IF NOT EXISTS proyecto (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, codigo VARCHAR(30) UNIQUE NOT NULL, idea_id BIGINT,
 modalidad VARCHAR(80), estado VARCHAR(40) NOT NULL DEFAULT 'PROPUESTA',
 fecha_aprobacion DATE, prorroga_count INT DEFAULT 0, fecha_inicio DATE,
 FOREIGN KEY (idea_id) REFERENCES idea(id)
);
CREATE TABLE IF NOT EXISTS integrante (
 proyecto_id BIGINT NOT NULL, estudiante_id BIGINT NOT NULL, fecha_vinculacion DATE NOT NULL,
 fecha_retiro DATE, PRIMARY KEY (proyecto_id,estudiante_id),
 FOREIGN KEY (proyecto_id) REFERENCES proyecto(id), FOREIGN KEY (estudiante_id) REFERENCES usuario(id)
);
CREATE TABLE IF NOT EXISTS asignacion (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, proyecto_id BIGINT NOT NULL, docente_id BIGINT NOT NULL,
 tipo VARCHAR(25) NOT NULL, estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
 fecha DATE DEFAULT (CURRENT_DATE), FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
 FOREIGN KEY (docente_id) REFERENCES usuario(id)
);
CREATE TABLE IF NOT EXISTS etapa (
 proyecto_id BIGINT NOT NULL, numero INT NOT NULL, nombre VARCHAR(80) NOT NULL,
 estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', fecha_limite DATE,
 fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(proyecto_id,numero), FOREIGN KEY(proyecto_id) REFERENCES proyecto(id)
);
CREATE TABLE IF NOT EXISTS documento (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, proyecto_id BIGINT NOT NULL, tipo VARCHAR(40) NOT NULL,
 url VARCHAR(500) NOT NULL, estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
 observaciones TEXT, firma_director BOOLEAN DEFAULT FALSE, firma_evaluador BOOLEAN DEFAULT FALSE,
 fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (proyecto_id) REFERENCES proyecto(id)
);
CREATE TABLE IF NOT EXISTS pago (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, proyecto_id BIGINT NOT NULL, estudiante_id BIGINT NOT NULL,
 tipo VARCHAR(40) NOT NULL, url_soporte VARCHAR(500) NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE', fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (proyecto_id) REFERENCES proyecto(id), FOREIGN KEY (estudiante_id) REFERENCES usuario(id)
);
CREATE TABLE IF NOT EXISTS evaluacion (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, proyecto_id BIGINT NOT NULL, docente_id BIGINT NOT NULL,
 fase VARCHAR(30) NOT NULL, concepto VARCHAR(40) NOT NULL, observaciones TEXT NOT NULL,
 firma BOOLEAN DEFAULT FALSE, fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (proyecto_id) REFERENCES proyecto(id), FOREIGN KEY (docente_id) REFERENCES usuario(id)
);
CREATE TABLE IF NOT EXISTS seguimiento (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, proyecto_id BIGINT NOT NULL, director_id BIGINT NOT NULL,
 fecha DATE NOT NULL, observaciones TEXT NOT NULL, FOREIGN KEY (proyecto_id) REFERENCES proyecto(id)
);
CREATE TABLE IF NOT EXISTS prorroga (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, proyecto_id BIGINT NOT NULL, carta_url VARCHAR(500) NOT NULL,
 pago_url VARCHAR(500) NOT NULL, estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
 dias INT DEFAULT 180, fecha_solicitud TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (proyecto_id) REFERENCES proyecto(id)
);
CREATE TABLE IF NOT EXISTS pqrs (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, usuario_id BIGINT NOT NULL, asunto VARCHAR(180) NOT NULL,
 descripcion TEXT NOT NULL, estado VARCHAR(30) NOT NULL DEFAULT 'ABIERTA', respuesta TEXT,
 fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
CREATE TABLE IF NOT EXISTS calendario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, periodo VARCHAR(20) NOT NULL, evento VARCHAR(100) NOT NULL,
 inicio DATE NOT NULL, fin DATE NOT NULL
);
CREATE TABLE IF NOT EXISTS notificacion (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, rol VARCHAR(20), usuario_id BIGINT,
 mensaje VARCHAR(500) NOT NULL, leida BOOLEAN DEFAULT FALSE, fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS historial (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, usuario_id BIGINT, entidad VARCHAR(40) NOT NULL,
 entidad_id BIGINT, accion VARCHAR(100) NOT NULL, detalle TEXT,
 fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS parametro (
 clave VARCHAR(80) PRIMARY KEY, valor VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS respaldo_referencia (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, entidad VARCHAR(40) NOT NULL,
 referencia VARCHAR(500) NOT NULL, fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Migración idempotente para bases existentes.
SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='usuario' AND column_name='especialidad');
SET @schema_sql := IF(@column_exists=0, 'ALTER TABLE usuario ADD COLUMN especialidad VARCHAR(120)', 'SELECT 1');
PREPARE schema_stmt FROM @schema_sql;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='usuario' AND column_name='carga_maxima');
SET @schema_sql := IF(@column_exists=0, 'ALTER TABLE usuario ADD COLUMN carga_maxima INT DEFAULT 5', 'SELECT 1');
PREPARE schema_stmt FROM @schema_sql;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='idea' AND column_name='fecha');
SET @schema_sql := IF(@column_exists=0, 'ALTER TABLE idea ADD COLUMN fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE schema_stmt FROM @schema_sql;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='proyecto' AND column_name='fecha_inicio');
SET @schema_sql := IF(@column_exists=0, 'ALTER TABLE proyecto ADD COLUMN fecha_inicio DATE', 'SELECT 1');
PREPARE schema_stmt FROM @schema_sql;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='etapa' AND column_name='nombre');
SET @schema_sql := IF(@column_exists=0, 'ALTER TABLE etapa ADD COLUMN nombre VARCHAR(80) NOT NULL DEFAULT 'Etapa'', 'SELECT 1');
PREPARE schema_stmt FROM @schema_sql;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='etapa' AND column_name='fecha_limite');
SET @schema_sql := IF(@column_exists=0, 'ALTER TABLE etapa ADD COLUMN fecha_limite DATE', 'SELECT 1');
PREPARE schema_stmt FROM @schema_sql;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='documento' AND column_name='firma_director');
SET @schema_sql := IF(@column_exists=0, 'ALTER TABLE documento ADD COLUMN firma_director BOOLEAN DEFAULT FALSE', 'SELECT 1');
PREPARE schema_stmt FROM @schema_sql;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='documento' AND column_name='firma_evaluador');
SET @schema_sql := IF(@column_exists=0, 'ALTER TABLE documento ADD COLUMN firma_evaluador BOOLEAN DEFAULT FALSE', 'SELECT 1');
PREPARE schema_stmt FROM @schema_sql;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;
SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pago' AND column_name='fecha');
SET @schema_sql := IF(@column_exists=0, 'ALTER TABLE pago ADD COLUMN fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE schema_stmt FROM @schema_sql;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;
INSERT IGNORE INTO parametro(clave,valor) VALUES
 ('equipo_maximo_propuesta','3'),('equipo_maximo_monografia','2'),('equipo_maximo_proyecto','4'),('docente_carga_maxima','5');
INSERT IGNORE INTO usuario(documento,nombre,correo,contrasena,rol,programa,nivel,porcentaje_creditos,carga_maxima)
VALUES ('1000','Administrador','admin@uts.edu.co','demo123','ADMIN',NULL,1,100,99),
('1001','Comité UTS','comite@uts.edu.co','demo123','COMITE',NULL,1,100,99),
('1002','Director Demo','director@uts.edu.co','demo123','DIRECTOR',NULL,1,100,5),
('1003','Evaluador Demo','evaluador@uts.edu.co','demo123','EVALUADOR',NULL,1,100,5),
('1004','Estudiante Demo','estudiante@uts.edu.co','demo123','ESTUDIANTE','Programación en Java',1,80,0);
INSERT IGNORE INTO idea(id,titulo,descripcion,nivel,estado,leida_por_comite)
VALUES (1,'Sistema de gestión de trabajos de grado','Plataforma web para centralizar el ciclo de vida del proyecto.',1,'DISPONIBLE',TRUE);