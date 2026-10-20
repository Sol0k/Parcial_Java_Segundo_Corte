<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="es"><head><meta charset="UTF-8"><title>Panel UTS</title><link rel="stylesheet" href="/app.css"></head>
<body><header><h1>Panel de trabajos de grado</h1><span>${user.nombre} · ${user.rol}</span></header><main class="grid">
<section class="card"><h2>Banco de ideas</h2><c:forEach var="idea" items="${ideas}"><article><h3>${idea.titulo}</h3><p>${idea.descripcion}</p><small>Nivel ${idea.nivel} · ${idea.estado}</small></article></c:forEach>
<form method="post" action="/ideas"><h3>Proponer idea</h3><input name="titulo" placeholder="Título" required><textarea name="descripcion" placeholder="Descripción" required></textarea><input name="nivel" type="number" min="1" required><button>Guardar idea</button></form></section>
<section class="card"><h2>Proyectos</h2><c:forEach var="p" items="${proyectos}"><article><strong>${p.codigo}</strong> · ${p.estado}<p>${p.modalidad}</p></article></c:forEach>
<h2>PQRS</h2><form method="post" action="/pqrs"><input name="asunto" placeholder="Asunto" required><textarea name="descripcion" placeholder="Descripción" required></textarea><button>Registrar PQRS</button></form></section>
</main></body></html>