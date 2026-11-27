package com.example.estudia.datos.local

import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.CategoriaArchivo
import com.example.estudia.modelo.Feriado
import com.example.estudia.modelo.Asistencia
import com.example.estudia.modelo.EstadoPlan
import com.example.estudia.modelo.Evaluacion
import com.example.estudia.modelo.MateriaPlan
import com.example.estudia.modelo.MesaFinal
import com.example.estudia.modelo.Fuente
import com.example.estudia.modelo.MapaMental
import com.example.estudia.modelo.codificarNodos
import com.example.estudia.modelo.decodificarNodos
import com.example.estudia.modelo.TipoFuente
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
    materias = materias.map { it.aModelo() }.sortedBy { it.nombre.lowercase() },
    planEstudio = planEstudio.map { it.aModelo() }.sortedWith(compareBy<MateriaPlan> { it.anio }.thenBy { it.id }),
    finales = finales.map { it.aModelo() }.sortedBy { it.fechaMesa },
    fuentes = fuentes.map { it.aModelo() }
        .sortedWith(compareBy<Fuente> { (it.listaAutores.firstOrNull() ?: it.titulo).lowercase() }.thenBy { it.anio ?: 0 }),
    // El que se tocó último, primero.
    mapas = mapas.map { it.aModelo() }.sortedWith(compareByDescending<MapaMental> { it.modificado }.thenByDescending { it.id })
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
        asistenciaMinima = materia.asistenciaMinima,
        promocional = materia.promocional,
        notaPromocion = materia.notaPromocion,
        tieneFinal = materia.tieneFinal,
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
        tarjetas = tarjetas.map { it.aModelo() }.sortedBy { it.id },
        evaluaciones = evaluaciones.map { it.aModelo() }.sortedBy { it.id }
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

fun EventoConTemas.aModelo(): Evento = evento.aModelo(
    temas.map { it.aModelo() }.sortedBy { it.id },
    asistencias.map { it.aModelo() }.sortedBy { it.fecha }
)

// Sin temas cuando viene de una consulta que no los trae (los recordatorios).
fun EventoEntity.aModelo(temas: List<Tema> = emptyList(), asistencias: List<Asistencia> = emptyList()): Evento = Evento(
    id = id,
    titulo = titulo,
    tipo = textoATipo(tipo),
    fecha = fecha,
    horaInicio = horaInicio,
    horaFin = horaFin,
    semanal = semanal,
    repetirHasta = repetirHasta,
    temas = temas,
    asistencias = asistencias,
    virtual = virtual,
    lugar = lugar,
    enlace = enlace
)

fun TemaEntity.aModelo(): Tema = Tema(id = id, titulo = titulo, estudiado = estudiado)

fun Tema.aEntidad(eventoId: Int): TemaEntity = TemaEntity(id = id, eventoId = eventoId, titulo = titulo, estudiado = estudiado)

fun NotaEntity.aModelo(archivos: List<Archivo> = emptyList()): Nota = Nota(
    id = id,
    titulo = titulo,
    contenido = contenido,
    fechaCreacion = fechaCreacion,
    esFavorita = esFavorita,
    archivos = archivos,
    fijada = fijada,
    color = color
)

fun ArchivoEntity.aModelo(): Archivo = Archivo(
    id = id,
    nombre = nombre,
    tipoMime = tipoMime,
    ruta = ruta,
    tamanoBytes = tamanoBytes,
    fechaAgregado = fechaAgregado,
    notaId = notaId,
    categoria = CategoriaArchivo.desdeTexto(categoria)
)

// ---------- Dominio -> entidad ----------
// Las entidades hijas necesitan saber a quién pertenecen (usuarioId /
// materiaId), dato que el modelo de dominio no guarda.

fun Materia.aEntidad(usuarioId: Int): MateriaEntity = MateriaEntity(
    id = id,
    usuarioId = usuarioId,
    nombre = nombre,
    color = color,
    archivada = archivada,
    asistenciaMinima = asistenciaMinima,
    promocional = promocional,
    notaPromocion = notaPromocion,
    tieneFinal = tieneFinal
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
    repetirHasta = repetirHasta,
    virtual = virtual,
    lugar = lugar,
    enlace = enlace
)

fun Nota.aEntidad(materiaId: Int): NotaEntity = NotaEntity(
    id = id,
    materiaId = materiaId,
    titulo = titulo,
    contenido = contenido,
    fechaCreacion = fechaCreacion,
    esFavorita = esFavorita,
    fijada = fijada,
    color = color
)

fun Archivo.aEntidad(materiaId: Int): ArchivoEntity = ArchivoEntity(
    id = id,
    materiaId = materiaId,
    notaId = notaId,
    nombre = nombre,
    tipoMime = tipoMime,
    ruta = ruta,
    tamanoBytes = tamanoBytes,
    fechaAgregado = fechaAgregado,
    categoria = categoria.name
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

// ---------- Versión 9 ----------

fun MateriaPlanEntity.aModelo(): MateriaPlan = MateriaPlan(
    id = id,
    nombre = nombre,
    anio = anio,
    estado = EstadoPlan.entries.firstOrNull { it.name == estado } ?: EstadoPlan.PENDIENTE,
    correlativas = textoAIds(correlativas),
    notaFinal = notaFinal
)

fun MateriaPlan.aEntidad(usuarioId: Int): MateriaPlanEntity = MateriaPlanEntity(
    id = id,
    usuarioId = usuarioId,
    nombre = nombre,
    anio = anio,
    estado = estado.name,
    correlativas = idsATexto(correlativas),
    notaFinal = notaFinal
)

// "3,5" <-> [3, 5]
fun idsATexto(ids: List<Int>): String = ids.distinct().joinToString(",")

fun textoAIds(texto: String): List<Int> = texto.split(",").mapNotNull { it.trim().toIntOrNull() }

fun AsistenciaEntity.aModelo(): Asistencia = Asistencia(id, fecha, presente)

fun Asistencia.aEntidad(eventoId: Int): AsistenciaEntity = AsistenciaEntity(id, eventoId, fecha, presente)

fun EvaluacionEntity.aModelo(): Evaluacion = Evaluacion(id, nombre, peso, nota)

fun Evaluacion.aEntidad(materiaId: Int): EvaluacionEntity = EvaluacionEntity(id, materiaId, nombre, peso, nota)

fun FinalEntity.aModelo(): MesaFinal =
    MesaFinal(id, materia, fechaMesa, cierreInscripcion, vencimientoRegularidad, inscripto, nota)

fun MesaFinal.aEntidad(usuarioId: Int): FinalEntity =
    FinalEntity(id, usuarioId, materia, fechaMesa, cierreInscripcion, vencimientoRegularidad, inscripto, nota)

fun FeriadoEntity.aModelo(): Feriado = Feriado(fecha, nombre)

fun Feriado.aEntidad(): FeriadoEntity = FeriadoEntity(fecha, nombre)

fun FuenteEntity.aModelo(): Fuente =
    Fuente(id, materia, TipoFuente.desdeTexto(tipo), autores, anio, titulo, origen, enlace, paginas, comentario)

fun Fuente.aEntidad(usuarioId: Int): FuenteEntity =
    FuenteEntity(id, usuarioId, materia, tipo.name, autores, anio, titulo, origen, enlace, paginas, comentario)

// Un mapa sin ideas (no debería pasar) recupera al menos la central.
fun MapaEntity.aModelo(): MapaMental {
    val ideas = decodificarNodos(nodos)
    return if (ideas.any { it.padreId == null }) MapaMental(id, materia, titulo, ideas, modificado)
    else MapaMental.nuevo(id, materia, titulo, modificado)
}

fun MapaMental.aEntidad(usuarioId: Int): MapaEntity =
    MapaEntity(id, usuarioId, materia, titulo, codificarNodos(nodos), modificado)
