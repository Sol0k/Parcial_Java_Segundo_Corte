USE trabajos_grado_uts;

CREATE TABLE IF NOT EXISTS usuario(
 id INT AUTO_INCREMENT PRIMARY KEY,
 documento VARCHAR(30) NOT NULL UNIQUE,
 nombre VARCHAR(150) NOT NULL,
 correo VARCHAR(150) NOT NULL UNIQUE,
 password_sha256 CHAR(64) NOT NULL,
 rol ENUM('ESTUDIANTE','DIRECTOR','EVALUADOR','COMITE','ADMINISTRADOR') NOT NULL,
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS estudiante(
 usuario_id INT PRIMARY KEY,
 programa VARCHAR(120) NOT NULL,
 nivel VARCHAR(30) NOT NULL,
 creditos_aprobados DECIMAL(5,2) NOT NULL,
 FOREIGN KEY(usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS docente(
 usuario_id INT PRIMARY KEY,
 tipo VARCHAR(30) NOT NULL,
 carga_maxima INT NOT NULL DEFAULT 5,
 FOREIGN KEY(usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS idea(
 id INT AUTO_INCREMENT PRIMARY KEY,
 titulo VARCHAR(180) NOT NULL,
 descripcion TEXT NOT NULL,
 nivel VARCHAR(30) NOT NULL,
 modalidad VARCHAR(60) NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
 creado_por INT NOT NULL,
 creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(creado_por) REFERENCES usuario(id),
 INDEX(estado,nivel)
);

CREATE TABLE IF NOT EXISTS proyecto(
 id INT AUTO_INCREMENT PRIMARY KEY,
 codigo VARCHAR(20) NOT NULL UNIQUE,
 titulo VARCHAR(180) NOT NULL,
 modalidad VARCHAR(60) NOT NULL,
 estado VARCHAR(40) NOT NULL,
 etapa TINYINT NOT NULL,
 idea_id INT NULL,
 creado_por INT NOT NULL,
 creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(idea_id) REFERENCES idea(id),
 FOREIGN KEY(creado_por) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS integrante(
 proyecto_id INT NOT NULL,
 usuario_id INT NOT NULL,
 PRIMARY KEY(proyecto_id,usuario_id),
 FOREIGN KEY(proyecto_id) REFERENCES proyecto(id),
 FOREIGN KEY(usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS asignacion(
 id INT AUTO_INCREMENT PRIMARY KEY,
 proyecto_id INT NOT NULL,
 docente_id INT NOT NULL,
 tipo VARCHAR(30) NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVA',
 asignado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(proyecto_id) REFERENCES proyecto(id),
 FOREIGN KEY(docente_id) REFERENCES docente(usuario_id)
);

CREATE TABLE IF NOT EXISTS documento(
 id INT AUTO_INCREMENT PRIMARY KEY,
 proyecto_id INT NOT NULL,
 tipo VARCHAR(40) NOT NULL,
 url VARCHAR(1000) NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
 observaciones TEXT,
 creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(proyecto_id) REFERENCES proyecto(id)
);

CREATE TABLE IF NOT EXISTS evaluacion(
 id INT AUTO_INCREMENT PRIMARY KEY,
 proyecto_id INT NOT NULL,
 evaluador_id INT NOT NULL,
 tipo VARCHAR(30) NOT NULL,
 concepto VARCHAR(40) NOT NULL,
 observaciones TEXT,
 creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(proyecto_id) REFERENCES proyecto(id),
 FOREIGN KEY(evaluador_id) REFERENCES docente(usuario_id)
);

CREATE TABLE IF NOT EXISTS pago(
 id INT AUTO_INCREMENT PRIMARY KEY,
 proyecto_id INT NOT NULL,
 tipo VARCHAR(60) NOT NULL,
 url VARCHAR(1000) NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
 observaciones TEXT,
 creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(proyecto_id) REFERENCES proyecto(id)
);

CREATE TABLE IF NOT EXISTS prorroga(
 id INT AUTO_INCREMENT PRIMARY KEY,
 proyecto_id INT NOT NULL,
 carta_url VARCHAR(1000) NOT NULL,
 pago_url VARCHAR(1000) NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
 observaciones TEXT,
 solicitado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(proyecto_id) REFERENCES proyecto(id)
);

CREATE TABLE IF NOT EXISTS pqrs(
 id INT AUTO_INCREMENT PRIMARY KEY,
 usuario_id INT NOT NULL,
 tipo VARCHAR(20) NOT NULL,
 asunto VARCHAR(180) NOT NULL,
 descripcion TEXT NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',
 respuesta TEXT,
 fecha_respuesta DATETIME,
 creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS historial(
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 usuario_id INT NULL,
 entidad VARCHAR(40) NOT NULL,
 entidad_id INT NOT NULL,
 accion VARCHAR(80) NOT NULL,
 estado_anterior VARCHAR(40),
 estado_nuevo VARCHAR(40),
 observaciones TEXT,
 creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS notificacion(
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 usuario_id INT NOT NULL,
 mensaje VARCHAR(500) NOT NULL,
 leida BOOLEAN NOT NULL DEFAULT FALSE,
 creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS calendario(
 id INT AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(100) NOT NULL,
 apertura DATE NOT NULL,
 cierre DATE NOT NULL,
 activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS parametro(
 clave VARCHAR(80) PRIMARY KEY,
 valor VARCHAR(255) NOT NULL
);

INSERT INTO parametro(clave,valor) VALUES
('creditos_minimos','75'),
('max_prorrogas','2'),
('pagina_proyectos','20')
ON DUPLICATE KEY UPDATE valor=VALUES(valor);

DROP PROCEDURE IF EXISTS agregar_columna_si_falta;
DELIMITER //
CREATE PROCEDURE agregar_columna_si_falta(
 IN tabla VARCHAR(64),
 IN columna VARCHAR(64),
 IN definicion VARCHAR(255)
)
BEGIN
 IF NOT EXISTS (
  SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=tabla AND COLUMN_NAME=columna
 ) THEN
  SET @sql_text = CONCAT('ALTER TABLE ',tabla,' ADD COLUMN ',columna,' ',definicion);
  PREPARE sentencia FROM @sql_text;
  EXECUTE sentencia;
  DEALLOCATE PREPARE sentencia;
 END IF;
END//
DELIMITER ;

CALL agregar_columna_si_falta('proyecto','creado_en','TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP');
CALL agregar_columna_si_falta('documento','creado_en','TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP');
CALL agregar_columna_si_falta('pago','creado_en','TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP');
CALL agregar_columna_si_falta('historial','creado_en','TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP');

DROP PROCEDURE agregar_columna_si_falta;

INSERT INTO usuario(documento,nombre,correo,password_sha256,rol)
VALUES('1000000000','Administrador UTS','admin@uts.edu.co',SHA2('CambiarEstaClave',256),'ADMINISTRADOR')
ON DUPLICATE KEY UPDATE activo=TRUE;