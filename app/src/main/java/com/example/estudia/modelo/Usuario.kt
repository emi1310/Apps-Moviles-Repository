package com.example.estudia.modelo

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.estudia.util.minutosDelDia

data class Usuario(
    val id: Int,
    val nombreUsuario: String,
    var contrasena: String? = null,
    val metodoAuth: MetodoAuth,
    var nombre: String,
    var carrera: String? = null,
    var anio: Int? = null,
    var email: String? = null,
    val materias: SnapshotStateList<Materia> = mutableStateListOf()
) {

    fun iniciarSesion(usuarioIngresado: String, contrasenaIngresada: String): Boolean {
        return nombreUsuario == usuarioIngresado && contrasena == contrasenaIngresada
    }

    fun contarNotas(): Int {
        return materias.sumOf { it.notas.size }
    }

    fun contarMaterias(): Int {
        return materias.count { !it.archivada }
    }

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
