package com.example.estudia.ui.navegacion

import android.content.Context
import android.content.Intent
import com.example.estudia.MainActivity

// Lo que se pide desde afuera de la app: un botón de un widget o la
// notificación del temporizador. MainActivity lo recibe en el Intent y
// AppNavegacion lo hace apenas hay una sesión iniciada.
enum class AccionExterna {
    TEMPORIZADOR, NUEVA_NOTA, NUEVO_EVENTO, NUEVA_MATERIA, ASISTENTE,
    HORARIO, CALENDARIO, MATERIAS, NOTAS, BUSCAR
}

const val EXTRA_ACCION = "com.example.estudia.ACCION"

// El Intent que abre la app y hace la acción.
fun intentDeAccion(context: Context, accion: AccionExterna): Intent =
    Intent(context, MainActivity::class.java)
        .setAction("com.example.estudia.accion.${accion.name}")   // distinto por acción
        .putExtra(EXTRA_ACCION, accion.name)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

fun accionDelIntent(intent: Intent?): AccionExterna? {
    intent ?: return null
    if (intent.getBooleanExtra(MainActivity.EXTRA_ABRIR_TEMPORIZADOR, false)) return AccionExterna.TEMPORIZADOR
    val nombre = intent.getStringExtra(EXTRA_ACCION) ?: return null
    return AccionExterna.entries.firstOrNull { it.name == nombre }
}
