USE trabajos_grado_uts;

-- Integración final del flujo de empresas de práctica y configuración institucional.
-- Compatible con bases existentes: no elimina datos ni repite columnas.

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

CALL agregar_columna_si_falta('proyecto','empresa_nit','VARCHAR(30) NULL');
CALL agregar_columna_si_falta('proyecto','creado_en','TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP');
CALL agregar_columna_si_falta('documento','creado_en','TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP');
CALL agregar_columna_si_falta('pago','creado_en','TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP');
CALL agregar_columna_si_falta('historial','estado_anterior','VARCHAR(40) NULL');
CALL agregar_columna_si_falta('historial','estado_nuevo','VARCHAR(40) NULL');

DROP PROCEDURE agregar_columna_si_falta;

INSERT IGNORE INTO parametro(clave,valor) VALUES
('creditos_minimos','75'),
('max_prorrogas','2'),
('dias_prorroga','180'),
('pagina_proyectos','20'),
('min_longitud_clave','8'),
('min_porcentaje_materias','75'),
('max_integrantes_estandar','2'),
('max_integrantes_interdisciplinario','4'),
('max_integrantes_pesado','3'),
('min_longitud_texto_idea','30'),
('min_seguimientos_director','2'),
('revision_propuesta_dias_habiles','3'),
('revision_informe_dias_habiles','5'),
('fc125_min_dias','90'),
('fc125_max_dias','180'),
('practica_plazo_dias','365'),
('dominio_correo_estudiante','@uts.edu.co');