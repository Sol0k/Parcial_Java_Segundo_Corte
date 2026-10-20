package co.edu.uts;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {
    private final JdbcTemplate db;
    public HomeController(JdbcTemplate db) { this.db = db; }

    @GetMapping({"/", "/login"})
    String login() { return "login"; }

    @PostMapping("/login")
    String authenticate(@RequestParam String correo, @RequestParam String contrasena,
                        HttpSession session, Model model) {
        var users = db.queryForList(
            "SELECT * FROM usuario WHERE correo=? AND contrasena=? AND activo=1",
            correo, contrasena);
        if (users.isEmpty()) {
            model.addAttribute("error", "Credenciales inválidas");
            return "login";
        }
        session.setAttribute("user", users.getFirst());
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    String dashboard(HttpSession session, Model model) {
        var user = (java.util.Map<String, Object>) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("ideas", db.queryForList(
            "SELECT * FROM idea ORDER BY id DESC LIMIT 20"));
        model.addAttribute("proyectos", db.queryForList(
            "SELECT * FROM proyecto ORDER BY id DESC LIMIT 20"));
        return "dashboard";
    }

    @PostMapping("/ideas")
    String createIdea(@RequestParam String titulo, @RequestParam String descripcion,
                      @RequestParam int nivel, HttpSession session) {
        if (session.getAttribute("user") == null) return "redirect:/login";
        db.update("INSERT INTO idea(titulo,descripcion,nivel,estado) VALUES(?,?,?,'PENDIENTE')",
            titulo, descripcion, nivel);
        return "redirect:/dashboard";
    }

    @PostMapping("/pqrs")
    String createPqrs(@RequestParam String asunto, @RequestParam String descripcion,
                      HttpSession session) {
        var user = (java.util.Map<String, Object>) session.getAttribute("user");
        if (user != null) db.update(
            "INSERT INTO pqrs(usuario_id,asunto,descripcion) VALUES(?,?,?)",
            user.get("id"), asunto, descripcion);
        return "redirect:/dashboard";
    }
}