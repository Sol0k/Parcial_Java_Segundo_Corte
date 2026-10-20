package co.edu.uts.web;

import co.edu.uts.data.Database;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

public class ActionBean {
    private static final Logger LOG = Logger.getLogger(ActionBean.class.getName());
    private HttpServletRequest request;
    private Map<String, Object> user;
    private String redirect;

    public void setRequest(HttpServletRequest request) {
        this.request = request;
        if (request != null) {
            user = (Map<String, Object>) request.getSession().getAttribute("user");
            Database.init();
        }
    }

    public String getRedirect() { return redirect; }

    private String parameter(String name) { return request.getParameter(name); }

    private void requireRole(String... roles) {
        if (user == null || Arrays.stream(roles).noneMatch(role -> role.equals(user.get("rol")))) {
            throw new SecurityException("No tienes permisos para esta acción.");
        }
    }

    public String process() {
        if (request == null || !"POST".equalsIgnoreCase(request.getMethod())) return "";
        if (!Objects.equals(request.getSession().getAttribute("csrf"), parameter("csrf"))) return "";

        try {
            String action = parameter("action");
            if ("createIdea".equals(action)) {
                requireRole("ESTUDIANTE", "ADMIN", "COMITE");
                Database.update("INSERT INTO idea(titulo,descripcion,nivel,estado,creador_id) VALUES(?,?,?,'PENDIENTE',?)",
                    parameter("titulo"), parameter("descripcion"), parameter("nivel"), user.get("id"));
            } else if ("selectIdea".equals(action)) {
                requireRole("ESTUDIANTE");
                Database.update("INSERT IGNORE INTO idea_estudiante(idea_id,estudiante_id) VALUES(?,?)",
                    parameter("ideaId"), user.get("id"));
            } else if ("createProject".equals(action)) {
                requireRole("ADMIN", "COMITE");
                Database.update("INSERT INTO proyecto(codigo,idea_id,modalidad,estado) VALUES(?,?,?,'ACTIVO')",
                    parameter("codigo"), parameter("ideaId"), parameter("modalidad"));
            } else if ("document".equals(action)) {
                requireRole("ESTUDIANTE", "ADMIN", "COMITE", "DIRECTOR");
                Database.update("INSERT INTO documento(proyecto_id,tipo,url) VALUES(?,?,?)",
                    parameter("proyectoId"), parameter("tipo"), parameter("url"));
            } else if ("pqrs".equals(action)) {
                Database.update("INSERT INTO pqrs(usuario_id,asunto,descripcion) VALUES(?,?,?)",
                    user.get("id"), parameter("asunto"), parameter("descripcion"));
            } else if ("createUser".equals(action)) {
                requireRole("ADMIN");
                Database.update("INSERT INTO usuario(documento,nombre,correo,contrasena,rol) VALUES(?,?,?,?,?)",
                    parameter("documento"), parameter("nombre"), parameter("correo"),
                    BCrypt.hashpw(parameter("contrasena"), BCrypt.gensalt(10)), parameter("rol"));
            } else if ("readNotifications".equals(action)) {
                Database.update("UPDATE notificacion SET leida=TRUE");
            }

            String target = parameter("returnPage");
            String actionName = parameter("action");
            if (target == null || target.startsWith("create") || target.startsWith("select")
                    || target.equals("document.jsp") || target.equals("evaluation.jsp")
                    || target.equals("extension.jsp") || target.equals("readNotifications.jsp")
                    || target.equals("pqrs.jsp") || target.equals("stage.jsp")) {
                target = switch (actionName) {
                    case "createIdea", "selectIdea" -> "ideas.jsp";
                    case "createProject" -> "proyectos.jsp";
                    case "document" -> "entregables.jsp";
                    case "evaluation" -> "evaluaciones.jsp";
                    case "extension" -> "solicitudes.jsp";
                    case "pqrs" -> "pqrs.jsp";
                    case "readNotifications" -> "notificaciones.jsp";
                    default -> "inicio.jsp";
                };
            }
            redirect = request.getContextPath() + "/" + target;
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "Error procesando acción de aplicación", exception);
        }
        return "";
    }
}
