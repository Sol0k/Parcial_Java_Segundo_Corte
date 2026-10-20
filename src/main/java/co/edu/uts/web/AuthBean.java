package co.edu.uts.web;

import co.edu.uts.data.Database;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

public class AuthBean {
    private static final Logger LOG = Logger.getLogger(AuthBean.class.getName());
    private HttpServletRequest request;
    private String error;

    public void setRequest(HttpServletRequest request) {
        this.request = request;
        if (request != null) Database.init();
    }

    public String getError() {
        return error;
    }

    public String process() {
        if (request == null) {
            error = "No se pudo inicializar la solicitud. Recarga la página.";
            LOG.warning("AuthBean recibió una petición nula");
            return null;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) return null;

        try {
            String email = request.getParameter("correo");
            String password = request.getParameter("contrasena");
            Map<String, Object> user = Database.one(
                "SELECT * FROM usuario WHERE LOWER(correo)=LOWER(?) AND activo=TRUE", email
            );

            if (user == null || password == null
                    || !BCrypt.checkpw(password, String.valueOf(user.get("contrasena")))) {
                error = "Credenciales inválidas.";
                return null;
            }

            request.getSession().invalidate();
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(1800);
            session.setAttribute("user", user);
            return request.getContextPath() + "/inicio.jsp";
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "Error procesando inicio de sesión", exception);
            error = "No fue posible iniciar sesión en este momento.";
            return null;
        }
    }

    public String logout() {
        if (request != null) request.getSession().invalidate();
        return request == null ? "/login.jsp" : request.getContextPath() + "/login.jsp";
    }
}
