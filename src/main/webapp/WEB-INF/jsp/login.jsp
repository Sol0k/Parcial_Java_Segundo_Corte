<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html><html lang="es"><head><meta charset="UTF-8"><title>UTS - Acceso</title><link rel="stylesheet" href="/app.css"></head>
<body><main class="card"><h1>Trabajos de grado UTS</h1><p>Ingresa con tu correo institucional.</p>
<form method="post" action="/login"><input name="correo" type="email" placeholder="correo@uts.edu.co" required>
<input name="contrasena" type="password" placeholder="Contraseña" required><button>Ingresar</button></form>
<p class="error">${error}</p></main></body></html>