package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.R
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.modelo.Usuario
import com.example.estudia.notificaciones.AvisosSistema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PerfilUiState(
    val usuario: Usuario? = null,
    val horasEstaSemana: Int = 0,
    val recordatoriosActivos: Boolean = false,
    val tema: PreferenciaTema = PreferenciaTema.SISTEMA
) {
    // Progreso de la meta semanal entre 0 y 1 (null si no hay meta).
    val progresoMeta: Float?
        get() {
            val meta = usuario?.metaHorasSemanales ?: return null
            if (meta <= 0) return null
            return (horasEstaSemana.toFloat() / meta).coerceIn(0f, 1f)
        }
}

// ViewModel de "Mi Perfil": datos, metas de estudio, recordatorios y tema.
// El permiso de notificaciones lo maneja la UI (necesita la Activity); el
// ViewModel solo guarda la elección del usuario.
class PerfilViewModel(
    private val repositorio: RepositorioEstudia,
    private val avisosSistema: AvisosSistema,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModelConAvisos() {

    val uiState: StateFlow<PerfilUiState> =
        combine(repositorio.usuarioActual, repositorio.recordatoriosActivos, repositorio.tema) { usuario, recordatorios, tema ->
            PerfilUiState(
                usuario = usuario,
                horasEstaSemana = usuario?.horasEstudioDeLaSemana(hoy()) ?: 0,
                recordatoriosActivos = recordatorios,
                tema = tema
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerfilUiState())

    fun editarPerfil(nombre: String, carrera: String?, anio: Int?, email: String?) {
        val usuario = uiState.value.usuario ?: return
        viewModelScope.launch {
            repositorio.editarPerfil(usuario.id, nombre.trim(), carrera, anio, email)
            avisar(Aviso(R.string.aviso_perfil_guardado))
        }
    }

    // horas null = sin meta.
    fun cambiarMetaSemanal(horas: Int?) {
        val usuario = uiState.value.usuario ?: return
        viewModelScope.launch {
            repositorio.cambiarMetaSemanal(usuario.id, horas?.takeIf { it > 0 })
        }
    }

    // Se llama solo cuando la UI ya tiene el permiso (o al desactivarlos).
    fun cambiarRecordatorios(activos: Boolean) {
        viewModelScope.launch {
            repositorio.cambiarRecordatorios(activos)
            if (activos) {
                val eventos = repositorio.usuarioActual.first()?.materias?.flatMap { it.eventos }.orEmpty()
                avisosSistema.activarRecordatorios(eventos)
            } else {
                avisosSistema.desactivarRecordatorios()
            }
            avisar(Aviso(if (activos) R.string.aviso_recordatorios_activados else R.string.aviso_recordatorios_desactivados))
        }
    }

    fun cambiarTema(tema: PreferenciaTema) {
        viewModelScope.launch {
            repositorio.cambiarTema(tema)
        }
    }

    fun avisarSinPermiso() {
        avisar(Aviso(R.string.aviso_sin_permiso_notificaciones))
    }
}
