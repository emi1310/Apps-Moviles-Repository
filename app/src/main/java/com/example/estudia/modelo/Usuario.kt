package com.example.estudia.modelo

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.estudia.util.minutosDelDia

// Representa al usuario de la app. Es dueño de todas sus Materias,
// y a través de ellas llega indirectamente a sus tareas, eventos y notas.
data class Usuario(
    val id: Int,
    val nombreUsuario: String,
    var contrasena: String? = null,   // null si la cuenta no usa contraseña
    val metodoAuth: MetodoAuth,
    var nombre: String,
    var carrera: String? = null,
    var anio: Int? = null,
    var email: String? = null,
    val materias: SnapshotStateList<Materia> = mutableStateListOf()
) {

    // Compara las credenciales ingresadas contra las de este usuario.
    // Devuelve true si coinciden (login exitoso).
    fun iniciarSesion(usuarioIngresado: String, contrasenaIngresada: String): Boolean {
        return nombreUsuario == usuarioIngresado && contrasena == contrasenaIngresada
    }

    // Cuenta el total de notas sumando las notas de todas las materias.
    fun contarNotas(): Int {
        return materias.sumOf { it.notas.size }
    }

    // Cuenta cuántas materias activas (no archivadas) tiene el usuario.
    fun contarMaterias(): Int {
        return materias.count { !it.archivada }
    }

    // Suma la duración real (horaFin - horaInicio) de todos los bloques
    // de estudio de todas las materias y la devuelve en horas.
    fun calcularHorasEstudio(): Int {
        var totalMinutos = 0
        for (materia in materias) {
            for (evento in materia.eventos) {
                if (evento.tipo == TipoEvento.BLOQUE_ESTUDIO) {
                    totalMinutos += minutosDelDia(evento.horaFin) - minutosDelDia(evento.horaInicio)
                }
            }
        }
        return totalMinutos / 60
    }
}
