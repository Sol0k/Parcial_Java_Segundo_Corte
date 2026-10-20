package co.edu.uts;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
public class HomeController {
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;
    public HomeController(JdbcTemplate db, PasswordEncoder passwords) { this.db = db; this.passwords = passwords; }

    @GetMapping({"/", "/login"}) String login() { return "login"; }

    @PostMapping("/login")
    String authenticate(@RequestParam String correo, @RequestParam String contrasena, HttpSession session, Model model) {
        var users = db.queryForList("SELECT * FROM usuario WHERE correo=? AND activo=1", correo);
        if (users.isEmpty()) { model.addAttribute("error", "Credenciales inválidas"); return "login"; }
        var account=users.getFirst(); String stored=String.valueOf(account.get("contrasena"));
        boolean valid=stored.startsWith("$2a$") ? passwords.matches(contrasena,stored) : stored.equals(contrasena);
        if(!valid){ model.addAttribute("error","Credenciales inválidas"); return "login"; }
        if(!stored.startsWith("$2a$")){ String encoded=passwords.encode(contrasena); db.update("UPDATE usuario SET contrasena=? WHERE id=?",encoded,account.get("id")); account.put("contrasena",encoded); }
        session.setAttribute("user", account); return "redirect:/dashboard";
    }

    @GetMapping("/logout") String logout(HttpSession session) { session.invalidate(); return "redirect:/login"; }

    @GetMapping("/dashboard")
    String dashboard(HttpSession session, Model model) {
        Map<String,Object> u=user(session); if (u==null) return "redirect:/login";
        model.addAttribute("user",u);
        model.addAttribute("ideas",db.queryForList("SELECT * FROM idea ORDER BY id DESC LIMIT 20"));
        model.addAttribute("proyectos",projectsFor((String)u.get("rol"),u));
        model.addAttribute("documentos",db.queryForList("SELECT * FROM documento ORDER BY id DESC LIMIT 20"));
        model.addAttribute("evaluaciones",db.queryForList("SELECT * FROM evaluacion ORDER BY id DESC LIMIT 20"));
        model.addAttribute("pqrs",db.queryForList("SELECT p.*,u.nombre usuario FROM pqrs p JOIN usuario u ON u.id=p.usuario_id ORDER BY p.id DESC LIMIT 20"));
        model.addAttribute("prorrogas",db.queryForList("SELECT * FROM prorroga ORDER BY id DESC LIMIT 20"));
        model.addAttribute("calendario",db.queryForList("SELECT * FROM calendario ORDER BY inicio LIMIT 20"));
        model.addAttribute("historial",db.queryForList("SELECT * FROM historial ORDER BY fecha DESC LIMIT 20"));
        model.addAttribute("usuarios",db.queryForList("SELECT id,nombre,correo,rol,activo FROM usuario ORDER BY nombre LIMIT 20"));
        return "dashboard";
    }

    @PostMapping("/ideas") String createIdea(@RequestParam String titulo,@RequestParam String descripcion,@RequestParam int nivel,HttpSession s) {
        Map<String,Object> u=user(s); if(u==null)return "redirect:/login";
        String correo=String.valueOf(u.get("correo"));
        if(!correo.endsWith("@uts.edu.co") || titulo.isBlank() || descripcion.isBlank() || nivel<1)return "redirect:/dashboard";
        if("ESTUDIANTE".equals(u.get("rol")) && !eligible(u))return "redirect:/dashboard";
        db.update("INSERT INTO idea(titulo,descripcion,nivel,estado,creador_id) VALUES(?,?,?,'PENDIENTE',?)",titulo.trim(),descripcion.trim(),nivel,u.get("id"));
        audit(u,"IDEA",null,"CREAR",titulo); notifyRoles("COMITE","Nueva idea pendiente: "+titulo); return "redirect:/dashboard";
    }

    @PostMapping("/ideas/{id}/decision") String decideIdea(@PathVariable long id,@RequestParam String estado,@RequestParam(defaultValue="") String observaciones,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        db.update("UPDATE idea SET estado=?,observaciones=?,leida_por_comite=TRUE WHERE id=?",estado,observaciones,id);
        audit(u,"IDEA",id,estado,observaciones); return "redirect:/dashboard";
    }

    @PostMapping("/proyectos") String createProject(@RequestParam long ideaId,@RequestParam String codigo,@RequestParam String modalidad,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        if(!Pattern.matches("[A-Z]{3}-\\d{4}\\.\\d-\\d{2}",codigo) || db.queryForObject("SELECT COUNT(*) FROM idea WHERE id=? AND estado='APROBADA'",Integer.class,ideaId)==0)return "redirect:/dashboard";
        db.update("INSERT INTO proyecto(codigo,idea_id,modalidad) VALUES(?,?,?)",codigo,ideaId,modalidad);
        audit(u,"PROYECTO",null,"CREAR",codigo); return "redirect:/dashboard";
    }

    @PostMapping("/equipos")
    String addMember(@RequestParam long proyectoId,@RequestParam long estudianteId,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"ESTUDIANTE","COMITE","ADMIN"))return "redirect:/dashboard";
        Integer started=db.queryForObject("SELECT COUNT(*) FROM etapa WHERE proyecto_id=? AND estado='EN_CURSO'",Integer.class,proyectoId);
        Integer exists=db.queryForObject("SELECT COUNT(*) FROM integrante WHERE proyecto_id=? AND estudiante_id=?",Integer.class,proyectoId,estudianteId);
        if((started!=null&&started>0)||(exists!=null&&exists>0))return "redirect:/dashboard";
        if(db.queryForObject("SELECT COUNT(*) FROM integrante WHERE estudiante_id=? AND fecha_retiro IS NULL",Integer.class,estudianteId)>0)return "redirect:/dashboard";
        db.update("INSERT INTO integrante(proyecto_id,estudiante_id,fecha_vinculacion) VALUES(?,?,CURRENT_DATE)",proyectoId,estudianteId);
        audit(u,"EQUIPO",proyectoId,"AGREGAR",String.valueOf(estudianteId)); return "redirect:/dashboard";
    }

    @PostMapping("/etapas")
    String stage(@RequestParam long proyectoId,@RequestParam int numero,@RequestParam String estado,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"DIRECTOR","COMITE","ADMIN"))return "redirect:/dashboard";
        if(numero<1||numero>6)return "redirect:/dashboard";
        db.update("INSERT INTO etapa(proyecto_id,numero,estado) VALUES(?,?,?) ON DUPLICATE KEY UPDATE estado=VALUES(estado)",proyectoId,numero,estado);
        audit(u,"ETAPA",proyectoId,"ACTUALIZAR","Etapa "+numero+" "+estado); return "redirect:/dashboard";
    }

    @PostMapping("/documentos/{id}/decision")
    String documentDecision(@PathVariable long id,@RequestParam String estado,@RequestParam(defaultValue="") String observaciones,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"DIRECTOR","COMITE","ADMIN"))return "redirect:/dashboard";
        if("RECHAZADO".equals(estado)&&observaciones.isBlank())return "redirect:/dashboard";
        db.update("UPDATE documento SET estado=?,observaciones=? WHERE id=?",estado,observaciones,id);
        audit(u,"DOCUMENTO",id,estado,observaciones); return "redirect:/dashboard";
    }

    @PostMapping("/pagos/{id}/decision")
    String paymentDecision(@PathVariable long id,@RequestParam String estado,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        db.update("UPDATE pago SET estado=? WHERE id=?",estado,id); audit(u,"PAGO",id,estado,""); return "redirect:/dashboard";
    }

    @PostMapping("/prorrogas/{id}/decision")
    String extensionDecision(@PathVariable long id,@RequestParam String estado,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        db.update("UPDATE prorroga SET estado=? WHERE id=?",estado,id); audit(u,"PRORROGA",id,estado,""); return "redirect:/dashboard";
    }

    @GetMapping("/health") @ResponseBody String health(){ return "ok"; }

    @GetMapping("/buscar")
    String search(@RequestParam(defaultValue="") String q,HttpSession s,Model model){
        Map<String,Object> u=user(s); if(u==null)return "redirect:/login";
        model.addAttribute("user",u); model.addAttribute("ideas",db.queryForList("SELECT * FROM idea WHERE titulo LIKE ? OR descripcion LIKE ? LIMIT 20","%"+q+"%","%"+q+"%"));
        model.addAttribute("proyectos",db.queryForList("SELECT * FROM proyecto WHERE codigo LIKE ? OR modalidad LIKE ? LIMIT 20","%"+q+"%","%"+q+"%"));
        model.addAttribute("documentos",java.util.List.of()); model.addAttribute("evaluaciones",java.util.List.of()); model.addAttribute("pqrs",java.util.List.of()); model.addAttribute("prorrogas",java.util.List.of()); model.addAttribute("calendario",java.util.List.of()); model.addAttribute("historial",java.util.List.of()); model.addAttribute("usuarios",java.util.List.of()); return "dashboard";
    }

    @PostMapping("/proyectos/{id}/asignacion") String assign(@PathVariable long id,@RequestParam long docenteId,@RequestParam String tipo,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        Integer count=db.queryForObject("SELECT COUNT(*) FROM asignacion WHERE docente_id=? AND estado='ACTIVA'",Integer.class,docenteId);
        Integer max=db.queryForObject("SELECT 5",Integer.class);
        if(count!=null&&max!=null&&count>=max)return "redirect:/dashboard";
        db.update("INSERT INTO asignacion(proyecto_id,docente_id,tipo) VALUES(?,?,?)",id,docenteId,tipo);
        audit(u,"PROYECTO",id,"ASIGNAR",tipo); return "redirect:/dashboard";
    }

    @PostMapping("/documentos") String document(@RequestParam long proyectoId,@RequestParam String tipo,@RequestParam String url,HttpSession s) {
        Map<String,Object> u=user(s); if(u==null)return "redirect:/login";
        db.update("INSERT INTO documento(proyecto_id,tipo,url) VALUES(?,?,?)",proyectoId,tipo,url);
        audit(u,"DOCUMENTO",proyectoId,"REGISTRAR",tipo); return "redirect:/dashboard";
    }

    @PostMapping("/pagos") String payment(@RequestParam long proyectoId,@RequestParam String tipo,@RequestParam String url,HttpSession s) {
        Map<String,Object> u=user(s); if(u==null)return "redirect:/login";
        db.update("INSERT INTO pago(proyecto_id,estudiante_id,tipo,url_soporte) VALUES(?,?,?,?)",proyectoId,u.get("id"),tipo,url);
        audit(u,"PAGO",proyectoId,"REGISTRAR",tipo); return "redirect:/dashboard";
    }

    @PostMapping("/evaluaciones") String evaluation(@RequestParam long proyectoId,@RequestParam String fase,@RequestParam String concepto,@RequestParam(defaultValue="") String observaciones,@RequestParam(defaultValue="false") boolean firma,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"DIRECTOR","EVALUADOR","COMITE","ADMIN"))return "redirect:/dashboard";
        if(concepto.toUpperCase().contains("NO")&&observaciones.isBlank())return "redirect:/dashboard";
        db.update("INSERT INTO evaluacion(proyecto_id,docente_id,fase,concepto,observaciones,firma) VALUES(?,?,?,?,?,?)",proyectoId,u.get("id"),fase,concepto,observaciones,firma);
        audit(u,"EVALUACION",proyectoId,"REGISTRAR",concepto); return "redirect:/dashboard";
    }

    @PostMapping("/prorrogas") String extension(@RequestParam long proyectoId,@RequestParam String cartaUrl,@RequestParam String pagoUrl,HttpSession s) {
        Map<String,Object> u=user(s); if(u==null)return "redirect:/login";
        Integer used=db.queryForObject("SELECT prorroga_count FROM proyecto WHERE id=?",Integer.class,proyectoId);
        if(used==null||used>=2)return "redirect:/dashboard";
        db.update("INSERT INTO prorroga(proyecto_id,carta_url,pago_url) VALUES(?,?,?)",proyectoId,cartaUrl,pagoUrl);
        db.update("UPDATE proyecto SET prorroga_count=prorroga_count+1 WHERE id=?",proyectoId);
        audit(u,"PRORROGA",proyectoId,"SOLICITAR","180 días"); return "redirect:/dashboard";
    }

    @PostMapping("/pqrs") String createPqrs(@RequestParam String asunto,@RequestParam String descripcion,HttpSession s) {
        Map<String,Object> u=user(s); if(u!=null)db.update("INSERT INTO pqrs(usuario_id,asunto,descripcion) VALUES(?,?,?)",u.get("id"),asunto,descripcion);
        return "redirect:/dashboard";
    }

    @PostMapping("/pqrs/{id}/responder") String answerPqrs(@PathVariable long id,@RequestParam String respuesta,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        db.update("UPDATE pqrs SET respuesta=?,estado='RESPONDIDA' WHERE id=?",respuesta,id);
        audit(u,"PQRS",id,"RESPONDER",respuesta); return "redirect:/dashboard";
    }

    @PostMapping("/calendario") String calendar(@RequestParam String periodo,@RequestParam String evento,@RequestParam String inicio,@RequestParam String fin,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        db.update("INSERT INTO calendario(periodo,evento,inicio,fin) VALUES(?,?,?,?)",periodo,evento,LocalDate.parse(inicio),LocalDate.parse(fin));
        audit(u,"CALENDARIO",null,"CREAR",evento); return "redirect:/dashboard";
    }

    @PostMapping("/usuarios") String createUser(@RequestParam String documento,@RequestParam String nombre,@RequestParam String correo,@RequestParam String rol,@RequestParam(defaultValue="demo123") String contrasena,HttpSession s) {
        Map<String,Object> u=user(s); if(!can(u,"ADMIN"))return "redirect:/dashboard";
        if(!correo.endsWith("@uts.edu.co"))return "redirect:/dashboard";
        db.update("INSERT INTO usuario(documento,nombre,correo,contrasena,rol) VALUES(?,?,?,?,?)",documento,nombre,correo,passwords.encode(contrasena),rol);
        audit(u,"USUARIO",null,"CREAR",correo); return "redirect:/dashboard";
    }

    private java.util.List<Map<String,Object>> projectsFor(String rol,Map<String,Object> u) {
        if("ESTUDIANTE".equals(rol))return db.queryForList("SELECT p.* FROM proyecto p JOIN integrante i ON i.proyecto_id=p.id WHERE i.estudiante_id=? LIMIT 20",u.get("id"));
        if("DIRECTOR".equals(rol)||"EVALUADOR".equals(rol))return db.queryForList("SELECT p.* FROM proyecto p JOIN asignacion a ON a.proyecto_id=p.id WHERE a.docente_id=? AND a.tipo=? LIMIT 20",u.get("id"),rol);
        return db.queryForList("SELECT * FROM proyecto ORDER BY id DESC LIMIT 20");
    }
    private boolean eligible(Map<String,Object> u){ Object v=u.get("porcentaje_creditos"); return v!=null && Double.parseDouble(v.toString())>=75; }
    private void notifyRoles(String role,String text){ db.update("INSERT INTO notificacion(rol,mensaje) SELECT ?,?",role,text); }
    private Map<String,Object> user(HttpSession s){return (Map<String,Object>)s.getAttribute("user");}
    private boolean can(Map<String,Object> u,String...roles){if(u==null)return false;for(String r:roles)if(r.equals(u.get("rol")))return true;return false;}
    private void audit(Map<String,Object> u,String e,Long id,String a,String d){db.update("INSERT INTO historial(usuario_id,entidad,entidad_id,accion,detalle) VALUES(?,?,?,?,?)",u.get("id"),e,id,a,d);}
}