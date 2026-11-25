package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
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
    val tema: PreferenciaTema = PreferenciaTema.SISTEMA,
    // Uri de la foto de perfil para mostrarla (null = sin foto).
    val fotoUri: String? = null,
    // Para el bloque "Tus logros".
    val logros: List<Logro> = emptyList(),
    val permisoArchivosPedido: Boolean = false
) {
    val logrosConseguidos: Int get() = logros.count { it.conseguido }

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
        combine(
            repositorio.usuarioActual,
            repositorio.recordatoriosActivos,
            repositorio.tema,
            repositorio.permisoArchivosPedido
        ) { usuario, recordatorios, tema, permisoPedido ->
            PerfilUiState(
                usuario = usuario,
                horasEstaSemana = usuario?.horasEstudioDeLaSemana(hoy()) ?: 0,
                recordatoriosActivos = recordatorios,
                tema = tema,
                fotoUri = usuario?.fotoPerfil?.let { repositorio.uriDeFoto(it) },
                logros = if (usuario == null) emptyList() else calcularLogros(usuario, hoy()),
                permisoArchivosPedido = permisoPedido
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerfilUiState())

    // uri: la foto de la cámara o de la galería (como texto); null = quitarla.
    fun cambiarFotoPerfil(uri: String?) {
        val usuario = uiState.value.usuario ?: return
        viewModelScope.launch {
            val guardada = repositorio.cambiarFotoPerfil(usuario.id, uri)
            avisar(
                Aviso(
                    when {
                        !guardada -> Mensaje.AVISO_ARCHIVO_ERROR
                        uri == null -> Mensaje.AVISO_FOTO_PERFIL_QUITADA
                        else -> Mensaje.AVISO_FOTO_PERFIL_GUARDADA
                    }
                )
            )
        }
    }

    fun marcarPermisoArchivosPedido() {
        viewModelScope.launch { repositorio.marcarPermisoArchivosPedido() }
    }

    fun editarPerfil(nombre: String, carrera: String?, anio: Int?, email: String?) {
        val usuario = uiState.value.usuario ?: return
        viewModelScope.launch {
            repositorio.editarPerfil(usuario.id, nombre.trim(), carrera, anio, email)
            avisar(Aviso(Mensaje.AVISO_PERFIL_GUARDADO))
        }
    }

    // ---------- Copia de seguridad ----------

    fun exportarCopia(uri: String) {
        viewModelScope.launch {
            avisar(Aviso(if (repositorio.exportarCopia(uri)) Mensaje.AVISO_COPIA_GUARDADA else Mensaje.AVISO_COPIA_ERROR))
        }
    }

    // Después de restaurar se vuelven a programar los avisos de los eventos
    // y tareas que trajo la copia.
    fun importarCopia(uri: String) {
        viewModelScope.launch {
            val restaurada = repositorio.importarCopia(uri)
            if (restaurada) {
                val materias = repositorio.usuarioActual.first()?.materias.orEmpty()
                avisosSistema.programarEventos(materias.flatMap { it.eventos })
                materias.flatMap { it.tareas }.forEach { avisosSistema.programarTarea(it) }
            }
            avisar(Aviso(if (restaurada) Mensaje.AVISO_COPIA_RESTAURADA else Mensaje.AVISO_COPIA_ERROR))
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
                val materias = repositorio.usuarioActual.first()?.materias.orEmpty()
                avisosSistema.activarRecordatorios(materias.flatMap { it.eventos }, materias.flatMap { it.tareas })
            } else {
                avisosSistema.desactivarRecordatorios()
            }
            avisar(Aviso(if (activos) Mensaje.AVISO_RECORDATORIOS_ACTIVADOS else Mensaje.AVISO_RECORDATORIOS_DESACTIVADOS))
        }
    }

    fun cambiarTema(tema: PreferenciaTema) {
        viewModelScope.launch {
            repositorio.cambiarTema(tema)
        }
    }

    fun avisarSinPermiso() {
        avisar(Aviso(Mensaje.AVISO_SIN_PERMISO_NOTIFICACIONES))
    }
}
