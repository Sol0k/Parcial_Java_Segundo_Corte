package co.edu.uts;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class ProcessScheduler {
    private final JdbcTemplate db;
    public ProcessScheduler(JdbcTemplate db){this.db=db;}

    @Scheduled(cron="0 0 8 * * *")
    public void reminders(){
        var rows=db.queryForList("SELECT evento,fin FROM calendario WHERE fin BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE,INTERVAL 7 DAY)");
        for(var row:rows) db.update("INSERT INTO notificacion(rol,mensaje) VALUES('COMITE',CONCAT('Próximo vencimiento: ',?, ' el ',?))",row.get("evento"),row.get("fin"));
        db.update("INSERT INTO respaldo_referencia(entidad,referencia) VALUES('SISTEMA','Respaldo lógico diario de referencias URL y eventos')");
    }
}