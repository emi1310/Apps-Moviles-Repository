package com.example.estudia.modelo

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
// Representa al usuario de la app. Es dueño de todas sus Materias,
// y a través de ellas llega indirectamente a sus tareas, eventos y notas.
data class Usuario(
    val id: Int,
    val nombreUsuario: String,
    var contrasena: String? = null,   // null si inició sesión con Google
    val metodoAuth: MetodoAuth,
    var nombre: String,
    var carrera: String? = null,
    var anio: Int? = null,
    var email: String? = null,
    var fotoPerfil: String? = null,
    val materias: SnapshotStateList<Materia> = mutableStateListOf()
) {

    // Compara las credenciales ingresadas contra las de este usuario.
    // Devuelve true si coinciden (login exitoso).
    fun iniciarSesion(usuarioIngresado: String, contrasenaIngresada: String): Boolean {
        return nombreUsuario == usuarioIngresado && contrasena == contrasenaIngresada
    }

    // Simula un login exitoso con Google (sin conexión real, ya que
    // requeriría una librería externa que no vamos a usar).
    fun iniciarSesionGoogle(): Boolean {
        return metodoAuth == MetodoAuth.GOOGLE
    }

    // Cuenta el total de notas sumando las notas de todas las materias.
    fun contarNotas(): Int {
        return materias.sumOf { it.notas.size }
    }

    // Cuenta cuántas materias activas (no archivadas) tiene el usuario.
    fun contarMaterias(): Int {
        return materias.count { !it.archivada }
    }

    // Suma la duración (en horas) de todos los bloques de estudio de esta semana.
    // NOTA: por ahora es un cálculo simplificado, lo vamos a refinar cuando
    // trabajemos la pantalla de Calendario.
    fun calcularHorasEstudioSemanal(): Int {
        var totalHoras = 0
        for (materia in materias) {
            for (evento in materia.eventos) {
                if (evento.tipo == TipoEvento.BLOQUE_ESTUDIO) {
                    totalHoras += 1 // simplificado: contamos cada bloque como 1 hora
                }
            }
        }
        return totalHoras
    }

    // Cuenta días consecutivos con actividad. Por ahora devuelve un valor fijo
    // de prueba; la lógica real de "días consecutivos" la resolvemos en el
    // bloque de Calendario, cuando tengamos fechas reales para comparar.
    fun calcularDiasRacha(): Int {
        return 0 // placeholder, se completa más adelante
    }
}