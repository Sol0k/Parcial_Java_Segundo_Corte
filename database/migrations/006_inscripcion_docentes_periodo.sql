USE trabajos_grado_uts;

CREATE TABLE IF NOT EXISTS inscripcion_docente_periodo(
 periodo_id INT NOT NULL,
 docente_id INT NOT NULL,
 puede_dirigir BOOLEAN NOT NULL DEFAULT FALSE,
 puede_evaluar BOOLEAN NOT NULL DEFAULT FALSE,
 limite_proyectos INT NOT NULL DEFAULT 0,
 PRIMARY KEY(periodo_id,docente_id),
 FOREIGN KEY(periodo_id) REFERENCES calendario(id),
 FOREIGN KEY(docente_id) REFERENCES docente(usuario_id)
);