package co.edu.uts;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Controller
public class HomeController {
    private static final Pattern CODE=Pattern.compile("[A-Z]{3}-\\d{4}\\.\\d-\\d{2}");
    private final JdbcTemplate db; private final PasswordEncoder encoder;
    public HomeController(JdbcTemplate db, PasswordEncoder encoder){this.db=db;this.encoder=encoder;}

    @GetMapping({"/","/login"}) String login(){return "login";}

    @PostMapping("/login")
    String authenticate(@RequestParam String correo,@RequestParam String contrasena,HttpSession s,Model m){
        var rows=db.queryForList("SELECT * FROM usuario WHERE correo=? AND activo=1",correo.trim().toLowerCase());
        if(rows.isEmpty()){m.addAttribute("error","Credenciales inválidas");return "login";}
        var account=rows.getFirst(); String stored=String.valueOf(account.get("contrasena"));
        boolean ok=stored.startsWith("$2a$")?encoder.matches(contrasena,stored):stored.equals(contrasena);
        if(!ok){m.addAttribute("error","Credenciales inválidas");return "login";}
        if(!stored.startsWith("$2a$")){String hash=encoder.encode(contrasena);db.update("UPDATE usuario SET contrasena=? WHERE id=?",hash,account.get("id"));account.put("contrasena",hash);}
        s.setAttribute("user",account);return "redirect:/dashboard";
    }
    @GetMapping("/logout") String logout(HttpSession s){s.invalidate();return "redirect:/login";}

    @GetMapping("/dashboard")
    String dashboard(@RequestParam(defaultValue="1") int page,HttpSession s,Model m){
        Map<String,Object> u=user(s);if(u==null)return "redirect:/login";
        int p=Math.max(1,page),offset=(p-1)*20; String role=String.valueOf(u.get("rol"));
        m.addAttribute("user",u);m.addAttribute("page",p);
        m.addAttribute("ideas",db.queryForList("SELECT * FROM idea ORDER BY fecha DESC LIMIT 20 OFFSET "+offset));
        m.addAttribute("proyectos",projects(role,u,offset));
        m.addAttribute("documentos",db.queryForList("SELECT * FROM documento ORDER BY fecha DESC LIMIT 20"));
        m.addAttribute("evaluaciones",db.queryForList("SELECT * FROM evaluacion ORDER BY fecha DESC LIMIT 20"));
        m.addAttribute("pqrs",db.queryForList("SELECT p.*,u.nombre usuario FROM pqrs p JOIN usuario u ON u.id=p.usuario_id ORDER BY p.fecha DESC LIMIT 20"));
        m.addAttribute("prorrogas",db.queryForList("SELECT * FROM prorroga ORDER BY fecha_solicitud DESC LIMIT 20"));
        m.addAttribute("calendario",db.queryForList("SELECT * FROM calendario ORDER BY inicio LIMIT 20"));
        m.addAttribute("historial",db.queryForList("SELECT * FROM historial ORDER BY fecha DESC LIMIT 20"));
        m.addAttribute("notificaciones",db.queryForList("SELECT * FROM notificacion WHERE (rol=? OR usuario_id=?) AND leida=FALSE ORDER BY fecha DESC LIMIT 20",role,u.get("id")));
        m.addAttribute("usuarios",db.queryForList("SELECT id,nombre,correo,rol,nivel,porcentaje_creditos,carga_maxima,activo FROM usuario ORDER BY nombre LIMIT 20"));
        m.addAttribute("pendientes",db.queryForList("SELECT COUNT(*) total FROM idea WHERE estado='PENDIENTE' UNION ALL SELECT COUNT(*) FROM documento WHERE estado='PENDIENTE' UNION ALL SELECT COUNT(*) FROM pago WHERE estado='PENDIENTE' UNION ALL SELECT COUNT(*) FROM prorroga WHERE estado='PENDIENTE'"));
        return "dashboard";
    }

    @PostMapping("/ideas")
    String createIdea(@RequestParam String titulo,@RequestParam String descripcion,@RequestParam int nivel,HttpSession s){
        Map<String,Object> u=user(s);if(u==null||!institutional(u)||titulo.isBlank()||descripcion.isBlank()||nivel<1)return "redirect:/dashboard";
        if("ESTUDIANTE".equals(u.get("rol"))&&(!eligible(u)||scalar("SELECT COUNT(*) FROM idea_estudiante WHERE estudiante_id=?",u.get("id"))>0))return "redirect:/dashboard";
        db.update("INSERT INTO idea(titulo,descripcion,nivel,estado,creador_id) VALUES(?,?,?,'PENDIENTE',?)",titulo.trim(),descripcion.trim(),nivel,u.get("id"));
        if("ESTUDIANTE".equals(u.get("rol")))db.update("INSERT INTO idea_estudiante(idea_id,estudiante_id) VALUES(LAST_INSERT_ID(),?)",u.get("id"));
        audit(u,"IDEA",null,"CREAR",titulo);notifyRole("COMITE","Nueva idea pendiente: "+titulo);return "redirect:/dashboard";
    }

    @PostMapping("/ideas/{id}/seleccionar")
    String selectIdea(@PathVariable long id,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"ESTUDIANTE"))return "redirect:/dashboard";
        if(!eligible(u)||scalar("SELECT COUNT(*) FROM idea_estudiante WHERE estudiante_id=?",u.get("id"))>0)return "redirect:/dashboard";
        Integer level=db.queryForObject("SELECT nivel FROM idea WHERE id=? AND estado='DISPONIBLE'",Integer.class,id);
        if(level==null||!level.equals(((Number)u.get("nivel")).intValue()))return "redirect:/dashboard";
        db.update("INSERT INTO idea_estudiante(idea_id,estudiante_id) VALUES(?,?)",id,u.get("id"));audit(u,"IDEA",id,"SELECCIONAR","");return "redirect:/dashboard";
    }

    @PostMapping("/ideas/{id}/decision")
    String decideIdea(@PathVariable long id,@RequestParam String estado,@RequestParam(defaultValue="") String observaciones,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"COMITE","ADMIN")||(!"APROBADA".equals(estado)&&!"RECHAZADA".equals(estado))||("RECHAZADA".equals(estado)&&observaciones.isBlank()))return "redirect:/dashboard";
        if(scalar("SELECT COUNT(*) FROM idea WHERE id=?",id)==0)return "redirect:/dashboard";
        db.update("UPDATE idea SET estado=?,observaciones=?,leida_por_comite=TRUE WHERE id=?",estado,observaciones,id);audit(u,"IDEA",id,estado,observaciones);return "redirect:/dashboard";
    }

    @PostMapping("/proyectos")
    String createProject(@RequestParam long ideaId,@RequestParam String codigo,@RequestParam String modalidad,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"COMITE","ADMIN")||!CODE.matcher(codigo).matches())return "redirect:/dashboard";
        Integer approved=db.queryForObject("SELECT COUNT(*) FROM idea WHERE id=? AND estado='APROBADA'",Integer.class,ideaId);
        if(approved==null||approved==0||scalar("SELECT COUNT(*) FROM proyecto WHERE codigo=?",codigo)>0)return "redirect:/dashboard";
        db.update("INSERT INTO proyecto(codigo,idea_id,modalidad,estado,fecha_aprobacion,fecha_inicio) VALUES(?,?,?,'PROPUESTA',CURRENT_DATE,NULL)",codigo,ideaId,modalidad);
        Long id=db.queryForObject("SELECT id FROM proyecto WHERE codigo=?",Long.class,codigo);
        String[] names={"Propuesta","Desarrollo","Seguimiento 1","Seguimiento 2","Informe final","Sustentación"};
        for(int i=0;i<6;i++)db.update("INSERT INTO etapa(proyecto_id,numero,nombre) VALUES(?,?,?)",id,i+1,names[i]);
        audit(u,"PROYECTO",id,"CREAR",codigo);return "redirect:/dashboard";
    }

    @PostMapping("/equipos")
    String addMember(@RequestParam long proyectoId,@RequestParam long estudianteId,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"ESTUDIANTE","COMITE","ADMIN"))return "redirect:/dashboard";
        if(db.queryForObject("SELECT fecha_inicio FROM proyecto WHERE id=?",Object.class,proyectoId)!=null)return "redirect:/dashboard";
        if(scalar("SELECT COUNT(*) FROM integrante WHERE proyecto_id=? AND estudiante_id=?",proyectoId,estudianteId)>0||scalar("SELECT COUNT(*) FROM idea_estudiante ies JOIN proyecto p ON p.idea_id=ies.idea_id WHERE p.id=? AND ies.estudiante_id=?",proyectoId,estudianteId)==0)return "redirect:/dashboard";
        if(scalar("SELECT COUNT(*) FROM integrante WHERE estudiante_id=? AND fecha_retiro IS NULL",estudianteId)>0)return "redirect:/dashboard";
        int max=parameter("equipo_maximo_"+String.valueOf(db.queryForObject("SELECT modalidad FROM proyecto WHERE id=?",String.class,proyectoId)).toLowerCase(),3);
        if(scalar("SELECT COUNT(*) FROM integrante WHERE proyecto_id=? AND fecha_retiro IS NULL",proyectoId)>=max)return "redirect:/dashboard";
        db.update("INSERT INTO integrante(proyecto_id,estudiante_id,fecha_vinculacion) VALUES(?,?,CURRENT_DATE)",proyectoId,estudianteId);audit(u,"EQUIPO",proyectoId,"AGREGAR",String.valueOf(estudianteId));return "redirect:/dashboard";
    }

    @PostMapping("/etapas")
    String stage(@RequestParam long proyectoId,@RequestParam int numero,@RequestParam String estado,@RequestParam(required=false) String fechaLimite,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"DIRECTOR","COMITE","ADMIN")||numero<1||numero>6)return "redirect:/dashboard";
        db.update("UPDATE etapa SET estado=?,fecha_limite=? WHERE proyecto_id=? AND numero=?",estado,fechaLimite==null||fechaLimite.isBlank()?null:LocalDate.parse(fechaLimite),proyectoId,numero);
        if("EN_CURSO".equals(estado))db.update("UPDATE proyecto SET fecha_inicio=COALESCE(fecha_inicio,CURRENT_DATE) WHERE id=?",proyectoId);
        audit(u,"ETAPA",proyectoId,"ACTUALIZAR",""+numero+" "+estado);return "redirect:/dashboard";
    }

    @PostMapping("/seguimientos")
    String followup(@RequestParam long proyectoId,@RequestParam String observaciones,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"DIRECTOR","COMITE","ADMIN")||observaciones.isBlank())return "redirect:/dashboard";
        if(!assigned(u,proyectoId,"DIRECTOR"))return "redirect:/dashboard";
        db.update("INSERT INTO seguimiento(proyecto_id,director_id,fecha,observaciones) VALUES(?,?,CURRENT_DATE,?)",proyectoId,u.get("id"),observaciones);
        audit(u,"SEGUIMIENTO",proyectoId,"REGISTRAR",observaciones);return "redirect:/dashboard";
    }

    @PostMapping("/documentos")
    String document(@RequestParam long proyectoId,@RequestParam String tipo,@RequestParam String url,HttpSession s){
        Map<String,Object> u=user(s);if(u==null||!url.matches("https?://.+"))return "redirect:/dashboard";
        db.update("INSERT INTO documento(proyecto_id,tipo,url) VALUES(?,?,?)",proyectoId,tipo,url);audit(u,"DOCUMENTO",proyectoId,"REGISTRAR",tipo);return "redirect:/dashboard";
    }

    @PostMapping("/documentos/{id}/decision")
    String documentDecision(@PathVariable long id,@RequestParam String estado,@RequestParam(defaultValue="") String observaciones,@RequestParam(defaultValue="false") boolean firma,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"DIRECTOR","EVALUADOR","COMITE","ADMIN")||("RECHAZADO".equals(estado)&&observaciones.isBlank()))return "redirect:/dashboard";
        db.update("UPDATE documento SET estado=?,observaciones=?,firma_director=CASE WHEN ?='DIRECTOR' THEN ? ELSE firma_director END,firma_evaluador=CASE WHEN ?='EVALUADOR' THEN ? ELSE firma_evaluador END WHERE id=?",estado,observaciones,u.get("rol"),firma,u.get("rol"),firma,id);
        audit(u,"DOCUMENTO",id,estado,observaciones);return "redirect:/dashboard";
    }

    @PostMapping("/pagos")
    String payment(@RequestParam long proyectoId,@RequestParam String tipo,@RequestParam String url,HttpSession s){
        Map<String,Object> u=user(s);if(u==null||!url.matches("https?://.+"))return "redirect:/dashboard";
        db.update("INSERT INTO pago(proyecto_id,estudiante_id,tipo,url_soporte) VALUES(?,?,?,?)",proyectoId,u.get("id"),tipo,url);audit(u,"PAGO",proyectoId,"REGISTRAR",tipo);return "redirect:/dashboard";
    }

    @PostMapping("/pagos/{id}/decision")
    String paymentDecision(@PathVariable long id,@RequestParam String estado,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"COMITE","ADMIN")||(!"APROBADO".equals(estado)&&!"RECHAZADO".equals(estado)))return "redirect:/dashboard";
        db.update("UPDATE pago SET estado=? WHERE id=?",estado,id);audit(u,"PAGO",id,estado,"");return "redirect:/dashboard";
    }

    @PostMapping("/evaluaciones")
    String evaluation(@RequestParam long proyectoId,@RequestParam String fase,@RequestParam String concepto,@RequestParam String observaciones,@RequestParam(defaultValue="false") boolean firma,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"DIRECTOR","EVALUADOR","COMITE","ADMIN")||observaciones.isBlank())return "redirect:/dashboard";
        if("EVALUADOR".equals(u.get("rol"))&&!assigned(u,proyectoId,"EVALUADOR")&&!assigned(u,proyectoId,"TERCER_EVALUADOR"))return "redirect:/dashboard";
        db.update("INSERT INTO evaluacion(proyecto_id,docente_id,fase,concepto,observaciones,firma) VALUES(?,?,?,?,?,?)",proyectoId,u.get("id"),fase,concepto,observaciones,firma);
        audit(u,"EVALUACION",proyectoId,"REGISTRAR",concepto);return "redirect:/dashboard";
    }

    @PostMapping("/asignaciones")
    String assignment(@RequestParam long proyectoId,@RequestParam long docenteId,@RequestParam String tipo,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        int max=parameter("docente_carga_maxima",5);Integer used=db.queryForObject("SELECT COUNT(*) FROM asignacion WHERE docente_id=? AND estado='ACTIVA'",Integer.class,docenteId);
        if(used!=null&&used>=max)return "redirect:/dashboard";
        db.update("INSERT INTO asignacion(proyecto_id,docente_id,tipo) VALUES(?,?,?)",proyectoId,docenteId,tipo);audit(u,"ASIGNACION",proyectoId,"CREAR",tipo);return "redirect:/dashboard";
    }

    @PostMapping("/prorrogas")
    String extension(@RequestParam long proyectoId,@RequestParam String cartaUrl,@RequestParam String pagoUrl,HttpSession s){
        Map<String,Object> u=user(s);if(u==null||!cartaUrl.matches("https?://.+")||!pagoUrl.matches("https?://.+"))return "redirect:/dashboard";
        Integer used=db.queryForObject("SELECT COUNT(*) FROM prorroga WHERE proyecto_id=?",Integer.class,proyectoId);
        if(used==null||used>=2)return "redirect:/dashboard";
        db.update("INSERT INTO prorroga(proyecto_id,carta_url,pago_url) VALUES(?,?,?)",proyectoId,cartaUrl,pagoUrl);audit(u,"PRORROGA",proyectoId,"SOLICITAR","180 días");return "redirect:/dashboard";
    }

    @PostMapping("/prorrogas/{id}/decision")
    String extensionDecision(@PathVariable long id,@RequestParam String estado,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        db.update("UPDATE prorroga SET estado=? WHERE id=?",estado,id);audit(u,"PRORROGA",id,estado,"");return "redirect:/dashboard";
    }

    @PostMapping("/pqrs")
    String createPqrs(@RequestParam String asunto,@RequestParam String descripcion,HttpSession s){
        Map<String,Object> u=user(s);if(u!=null&&!asunto.isBlank()&&!descripcion.isBlank())db.update("INSERT INTO pqrs(usuario_id,asunto,descripcion) VALUES(?,?,?)",u.get("id"),asunto,descripcion);
        return "redirect:/dashboard";
    }

    @PostMapping("/pqrs/{id}/responder")
    String answerPqrs(@PathVariable long id,@RequestParam String respuesta,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"COMITE","ADMIN")||respuesta.isBlank())return "redirect:/dashboard";
        db.update("UPDATE pqrs SET respuesta=?,estado='RESPONDIDA' WHERE id=?",respuesta,id);audit(u,"PQRS",id,"RESPONDER",respuesta);return "redirect:/dashboard";
    }

    @PostMapping("/calendario")
    String calendar(@RequestParam String periodo,@RequestParam String evento,@RequestParam String inicio,@RequestParam String fin,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        db.update("INSERT INTO calendario(periodo,evento,inicio,fin) VALUES(?,?,?,?)",periodo,evento,LocalDate.parse(inicio),LocalDate.parse(fin));audit(u,"CALENDARIO",null,"CREAR",evento);return "redirect:/dashboard";
    }

    @PostMapping("/usuarios")
    String createUser(@RequestParam String documento,@RequestParam String nombre,@RequestParam String correo,@RequestParam String rol,@RequestParam String contrasena,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"ADMIN")||!correo.endsWith("@uts.edu.co"))return "redirect:/dashboard";
        db.update("INSERT INTO usuario(documento,nombre,correo,contrasena,rol) VALUES(?,?,?,?,?)",documento,nombre,correo.toLowerCase(),encoder.encode(contrasena),rol);audit(u,"USUARIO",null,"CREAR",correo);return "redirect:/dashboard";
    }

    @PostMapping("/usuarios/{id}/estado")
    String userState(@PathVariable long id,@RequestParam boolean activo,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"ADMIN")||id == ((Number)u.get("id")).longValue())return "redirect:/dashboard";
        if(scalar("SELECT COUNT(*) FROM asignacion WHERE docente_id=? AND estado='ACTIVA'",id)>0&& !activo)return "redirect:/dashboard";
        db.update("UPDATE usuario SET activo=? WHERE id=?",activo,id);audit(u,"USUARIO",id,activo?"ACTIVAR":"DESACTIVAR","");return "redirect:/dashboard";
    }

    @PostMapping("/parametros")
    String parameter(@RequestParam String clave,@RequestParam String valor,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        db.update("INSERT INTO parametro(clave,valor) VALUES(?,?) ON DUPLICATE KEY UPDATE valor=VALUES(valor)",clave,valor);audit(u,"PARAMETRO",null,"ACTUALIZAR",clave);return "redirect:/dashboard";
    }

    @PostMapping("/paso-a-grado")
    String degree(@RequestParam long id,HttpSession s){
        Map<String,Object> u=user(s);if(!can(u,"COMITE","ADMIN"))return "redirect:/dashboard";
        Integer stages=db.queryForObject("SELECT COUNT(*) FROM etapa WHERE proyecto_id=? AND estado='FINALIZADA'",Integer.class,id);
        Integer docs=db.queryForObject("SELECT COUNT(*) FROM documento WHERE proyecto_id=? AND estado='APROBADO' AND firma_director=TRUE AND firma_evaluador=TRUE",Integer.class,id);
        Integer payments=db.queryForObject("SELECT COUNT(*) FROM pago WHERE proyecto_id=? AND estado='APROBADO'",Integer.class,id);
        Integer followups=db.queryForObject("SELECT COUNT(*) FROM seguimiento WHERE proyecto_id=?",Integer.class,id);
        if(stages==null||stages<6||docs==null||docs<1||payments==null||payments<1||followups==null||followups<2)return "redirect:/dashboard";
        db.update("UPDATE proyecto SET estado='LISTO_PARA_GRADO' WHERE id=?",id);audit(u,"PROYECTO",id,"PASO_A_GRADO","Requisitos completos");return "redirect:/dashboard";
    }

    @GetMapping("/buscar")
    String search(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page,HttpSession s,Model m){
        Map<String,Object> u=user(s);if(u==null)return "redirect:/login";int offset=Math.max(0,page-1)*20;
        m.addAttribute("user",u);m.addAttribute("ideas",db.queryForList("SELECT * FROM idea WHERE titulo LIKE ? OR descripcion LIKE ? LIMIT 20 OFFSET "+offset,"%"+q+"%","%"+q+"%"));
        m.addAttribute("proyectos",projectsSearch(q,offset));m.addAttribute("documentos",List.of());m.addAttribute("evaluaciones",List.of());m.addAttribute("pqrs",List.of());m.addAttribute("prorrogas",List.of());m.addAttribute("calendario",List.of());m.addAttribute("historial",List.of());m.addAttribute("usuarios",List.of());m.addAttribute("notificaciones",List.of());return "dashboard";
    }

    @GetMapping("/reportes/proyectos") @ResponseBody
    String report(HttpSession s){
        if(!can(user(s),"COMITE","ADMIN"))return "No autorizado";
        StringBuilder out=new StringBuilder("codigo,modalidad,estado,fecha_aprobacion\n");
        db.queryForList("SELECT codigo,modalidad,estado,fecha_aprobacion FROM proyecto ORDER BY codigo").forEach(r->out.append(r.get("codigo")).append(',').append(r.get("modalidad")).append(',').append(r.get("estado")).append(',').append(r.get("fecha_aprobacion")).append('\n'));
        return out.toString();
    }

    @PostMapping("/notificaciones/leidas") String readNotifications(HttpSession s){Map<String,Object> u=user(s);if(u!=null)db.update("UPDATE notificacion SET leida=TRUE WHERE rol=? OR usuario_id=?",u.get("rol"),u.get("id"));return "redirect:/dashboard";}
    @GetMapping("/health") @ResponseBody String health(){return "ok";}

    private boolean institutional(Map<String,Object> u){return String.valueOf(u.get("correo")).toLowerCase().endsWith("@uts.edu.co");}
    private boolean eligible(Map<String,Object> u){return u.get("porcentaje_creditos")!=null&&Double.parseDouble(u.get("porcentaje_creditos").toString())>=75;}
    private boolean assigned(Map<String,Object> u,long project,String type){return scalar("SELECT COUNT(*) FROM asignacion WHERE proyecto_id=? AND docente_id=? AND tipo=? AND estado='ACTIVA'",project,u.get("id"),type)>0;}
    private int parameter(String key,int fallback){Object v=scalar("SELECT valor FROM parametro WHERE clave=?",key);return v==null?fallback:Integer.parseInt(v.toString());}
    private long scalar(String sql,Object...args){Object v=db.queryForObject(sql,Object.class,args);return v==null?0:((Number)v).longValue();}
    private Map<String,Object> user(HttpSession s){return (Map<String,Object>)s.getAttribute("user");}
    private boolean can(Map<String,Object> u,String...roles){if(u==null)return false;for(String role:roles)if(role.equals(u.get("rol")))return true;return false;}
    private List<Map<String,Object>> projects(String role,Map<String,Object> u,int offset){
        if("ESTUDIANTE".equals(role))return db.queryForList("SELECT p.* FROM proyecto p JOIN integrante i ON i.proyecto_id=p.id WHERE i.estudiante_id=? LIMIT 20 OFFSET "+offset,u.get("id"));
        if("DIRECTOR".equals(role)||"EVALUADOR".equals(role))return db.queryForList("SELECT p.* FROM proyecto p JOIN asignacion a ON a.proyecto_id=p.id WHERE a.docente_id=? AND a.tipo=? LIMIT 20 OFFSET "+offset,u.get("id"),role);
        return db.queryForList("SELECT * FROM proyecto ORDER BY id DESC LIMIT 20 OFFSET "+offset);
    }
    private List<Map<String,Object>> projectsSearch(String q,int offset){return db.queryForList("SELECT * FROM proyecto WHERE codigo LIKE ? OR modalidad LIKE ? OR estado LIKE ? ORDER BY id DESC LIMIT 20 OFFSET "+offset,"%"+q+"%","%"+q+"%","%"+q+"%");}
    private void notifyRole(String role,String message){db.update("INSERT INTO notificacion(rol,mensaje) VALUES(?,?)",role,message);}
    private void audit(Map<String,Object> u,String entity,Long id,String action,String detail){db.update("INSERT INTO historial(usuario_id,entidad,entidad_id,accion,detalle) VALUES(?,?,?,?,?)",u.get("id"),entity,id,action,detail);}
}