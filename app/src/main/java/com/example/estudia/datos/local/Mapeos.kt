package com.example.estudia.datos.local

import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.Subtarea
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.modelo.Tema
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario

// Conversiones entre las entidades de Room (forma en que se guardan los
// datos) y el modelo de dominio (forma en que la app los usa). Las usa
// solamente el Repository, así ni el ViewModel ni la UI conocen Room.

// ---------- Entidad -> dominio ----------

// La contraseña (hash) no pasa al modelo de dominio.
fun UsuarioConMaterias.aModelo(): Usuario = Usuario(
    id = usuario.id,
    nombreUsuario = usuario.nombreUsuario,
    metodoAuth = textoAMetodoAuth(usuario.metodoAuth),
    nombre = usuario.nombre,
    carrera = usuario.carrera,
    anio = usuario.anio,
    email = usuario.email,
    metaHorasSemanales = usuario.metaHorasSemanales,
    fotoPerfil = usuario.fotoPerfil,
    materias = materias.map { it.aModelo() }.sortedBy { it.nombre.lowercase() }
)

// Los archivos llegan todos juntos: los que tienen notaId van a su nota,
// y los demás quedan como archivos de la materia.
fun MateriaConDetalle.aModelo(): Materia {
    val archivosModelo = archivos.map { it.aModelo() }
    return Materia(
        id = materia.id,
        nombre = materia.nombre,
        color = materia.color,
        archivada = materia.archivada,
        // Pendientes primero; entre ellas, las de prioridad alta y después
        // las que vencen antes (las que no tienen fecha, al final).
        tareas = tareas.map { it.aModelo() }
            .sortedWith(
                compareBy<Tarea> { it.completada }
                    .thenByDescending { it.prioridad.peso }
                    .thenBy { it.fechaLimite == null }
                    .thenBy { it.fechaLimite }
            ),
        eventos = eventos.map { it.aModelo() }.sortedBy { it.inicio },
        notas = notas.map { nota ->
            nota.aModelo(archivosModelo.filter { it.notaId == nota.id })
        },
        archivos = archivosModelo.filter { it.notaId == null },
        calificaciones = calificaciones.map { it.aModelo() }.sortedBy { it.fecha },
        tarjetas = tarjetas.map { it.aModelo() }.sortedBy { it.id }
    )
}

fun TarjetaEntity.aModelo(): Tarjeta = Tarjeta(id, pregunta, respuesta, caja, proximoRepaso)

fun Tarjeta.aEntidad(materiaId: Int): TarjetaEntity = TarjetaEntity(id, materiaId, pregunta, respuesta, caja, proximoRepaso)

fun TareaConSubtareas.aModelo(): Tarea = tarea.aModelo(subtareas.map { it.aModelo() }.sortedBy { it.id })

// Sin subtareas cuando viene de una consulta que no las trae (el buscador).
fun TareaEntity.aModelo(subtareas: List<Subtarea> = emptyList()): Tarea = Tarea(
    id = id,
    titulo = titulo,
    fechaLimite = fechaLimite,
    completada = completada,
    prioridad = Prioridad.desdePeso(prioridad),
    subtareas = subtareas,
    avisoDiasAntes = avisoDiasAntes
)

fun SubtareaEntity.aModelo(): Subtarea = Subtarea(id = id, titulo = titulo, completada = completada)

fun Subtarea.aEntidad(tareaId: Int): SubtareaEntity =
    SubtareaEntity(id = id, tareaId = tareaId, titulo = titulo, completada = completada)

fun EventoConTemas.aModelo(): Evento = evento.aModelo(temas.map { it.aModelo() }.sortedBy { it.id })

// Sin temas cuando viene de una consulta que no los trae (los recordatorios).
fun EventoEntity.aModelo(temas: List<Tema> = emptyList()): Evento = Evento(
    id = id,
    titulo = titulo,
    tipo = textoATipo(tipo),
    fecha = fecha,
    horaInicio = horaInicio,
    horaFin = horaFin,
    semanal = semanal,
    repetirHasta = repetirHasta,
    temas = temas
)

fun TemaEntity.aModelo(): Tema = Tema(id = id, titulo = titulo, estudiado = estudiado)

fun Tema.aEntidad(eventoId: Int): TemaEntity = TemaEntity(id = id, eventoId = eventoId, titulo = titulo, estudiado = estudiado)

fun NotaEntity.aModelo(archivos: List<Archivo> = emptyList()): Nota = Nota(
    id = id,
    titulo = titulo,
    contenido = contenido,
    fechaCreacion = fechaCreacion,
    esFavorita = esFavorita,
    archivos = archivos
)

fun ArchivoEntity.aModelo(): Archivo = Archivo(
    id = id,
    nombre = nombre,
    tipoMime = tipoMime,
    ruta = ruta,
    tamanoBytes = tamanoBytes,
    fechaAgregado = fechaAgregado,
    notaId = notaId
)

// ---------- Dominio -> entidad ----------
// Las entidades hijas necesitan saber a quién pertenecen (usuarioId /
// materiaId), dato que el modelo de dominio no guarda.

fun Materia.aEntidad(usuarioId: Int): MateriaEntity = MateriaEntity(
    id = id,
    usuarioId = usuarioId,
    nombre = nombre,
    color = color,
    archivada = archivada
)

fun Tarea.aEntidad(materiaId: Int): TareaEntity = TareaEntity(
    id = id,
    materiaId = materiaId,
    titulo = titulo,
    fechaLimite = fechaLimite,
    completada = completada,
    prioridad = prioridad.peso,
    avisoDiasAntes = avisoDiasAntes
)

fun Evento.aEntidad(materiaId: Int): EventoEntity = EventoEntity(
    id = id,
    materiaId = materiaId,
    titulo = titulo,
    tipo = tipoATexto(tipo),
    fecha = fecha,
    horaInicio = horaInicio,
    horaFin = horaFin,
    semanal = semanal,
    repetirHasta = repetirHasta
)

fun Nota.aEntidad(materiaId: Int): NotaEntity = NotaEntity(
    id = id,
    materiaId = materiaId,
    titulo = titulo,
    contenido = contenido,
    fechaCreacion = fechaCreacion,
    esFavorita = esFavorita
)

fun Archivo.aEntidad(materiaId: Int): ArchivoEntity = ArchivoEntity(
    id = id,
    materiaId = materiaId,
    notaId = notaId,
    nombre = nombre,
    tipoMime = tipoMime,
    ruta = ruta,
    tamanoBytes = tamanoBytes,
    fechaAgregado = fechaAgregado
)

// ---------- Tipos que se guardan como texto ----------

fun tipoATexto(tipo: TipoEvento): String {
    return when (tipo) {
        TipoEvento.CLASE -> "CLASE"
        TipoEvento.EXAMEN -> "EXAMEN"
        TipoEvento.ENTREGA -> "ENTREGA"
        else -> "BLOQUE_ESTUDIO"
    }
}

fun textoATipo(texto: String): TipoEvento {
    return when (texto) {
        "CLASE" -> TipoEvento.CLASE
        "EXAMEN" -> TipoEvento.EXAMEN
        "ENTREGA" -> TipoEvento.ENTREGA
        else -> TipoEvento.BLOQUE_ESTUDIO
    }
}

fun metodoAuthATexto(metodo: MetodoAuth): String {
    return if (metodo == MetodoAuth.GOOGLE) "GOOGLE" else "LOCAL"
}

private fun textoAMetodoAuth(texto: String): MetodoAuth {
    return if (texto == "GOOGLE") MetodoAuth.GOOGLE else MetodoAuth.LOCAL
}

fun CalificacionEntity.aModelo(): Calificacion = Calificacion(
    id = id,
    titulo = titulo,
    nota = nota,
    fecha = fecha
)

fun Calificacion.aEntidad(materiaId: Int): CalificacionEntity = CalificacionEntity(
    id = id,
    materiaId = materiaId,
    titulo = titulo,
    nota = nota,
    fecha = fecha
)
