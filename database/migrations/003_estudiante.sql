USE trabajos_grado_uts;
CREATE TABLE IF NOT EXISTS integrante(proyecto_id INT NOT NULL,usuario_id INT NOT NULL,PRIMARY KEY(proyecto_id,usuario_id),FOREIGN KEY(proyecto_id) REFERENCES proyecto(id),FOREIGN KEY(usuario_id) REFERENCES usuario(id));
CREATE TABLE IF NOT EXISTS prorroga(id INT AUTO_INCREMENT PRIMARY KEY,proyecto_id INT NOT NULL,carta_url VARCHAR(1000) NOT NULL,pago_url VARCHAR(1000) NOT NULL,estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',observaciones TEXT,solicitado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(proyecto_id) REFERENCES proyecto(id));
CREATE TABLE IF NOT EXISTS calendario(id INT AUTO_INCREMENT PRIMARY KEY,nombre VARCHAR(100) NOT NULL,apertura DATE NOT NULL,cierre DATE NOT NULL,activo BOOLEAN DEFAULT TRUE);
CREATE TABLE IF NOT EXISTS parametro(clave VARCHAR(80) PRIMARY KEY,valor VARCHAR(255) NOT NULL);
INSERT IGNORE INTO parametro(clave,valor) VALUES ('creditos_minimos','75'),('max_prorrogas','2'),('pagina_proyectos','20');