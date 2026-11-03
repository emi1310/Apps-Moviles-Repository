package com.example.estudia.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.R
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.modelo.Usuario
import com.example.estudia.notificaciones.AvisosSistema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la sesión que observa la navegación. Se usa sealed interface
// (alternativa 2 del apunte de MVVM) para que no se puedan combinar
// estados incoherentes, y el when de la UI sea exhaustivo.
sealed interface SesionUiState {
    // Todavía no se leyó la sesión guardada (se muestra un indicador de carga).
    data object Cargando : SesionUiState
    data object SinSesion : SesionUiState
    data class Activa(val usuario: Usuario) : SesionUiState
}

// Estado de las pantallas de Login y Registro.
// sesionIniciada funciona como evento: la UI navega a Inicio y avisa al
// ViewModel con consumirSesionIniciada() para que no se repita.
data class AuthUiState(
    @param:StringRes val error: Int? = null,
    val procesando: Boolean = false,
    val sesionIniciada: Boolean = false
)

// ViewModel de la sesión (vive mientras vive la Activity): login, registro,
// cierre de sesión y el tema elegido. Las pantallas de la app tienen cada
// una su propio ViewModel.
class SesionViewModel(
    private val repositorio: RepositorioEstudia,
    private val avisosSistema: AvisosSistema
) : ViewModel() {

    val sesionUiState: StateFlow<SesionUiState> = repositorio.usuarioActual
        .map { usuario ->
            if (usuario == null) SesionUiState.SinSesion else SesionUiState.Activa(usuario)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SesionUiState.Cargando
        )

    // Solo los valores guardados (sin el SISTEMA inicial de stateIn): los usa
    // MainActivity para avisarle al sistema el tema elegido.
    val temaGuardado: Flow<PreferenciaTema> = repositorio.tema

    val tema: StateFlow<PreferenciaTema> = repositorio.tema
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferenciaTema.SISTEMA)

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repositorio.crearDatosDePruebaSiHaceFalta()
        }
    }

    fun iniciarSesion(nombreUsuario: String, contrasena: String) {
        if (nombreUsuario.isBlank() || contrasena.isBlank()) {
            _authUiState.value = AuthUiState(error = R.string.error_completar_campos)
            return
        }
        _authUiState.update { it.copy(procesando = true, error = null) }
        viewModelScope.launch {
            val correcto = repositorio.iniciarSesion(nombreUsuario.trim(), contrasena)
            // Los avisos de sus eventos se programan de nuevo (por si cambió de usuario).
            if (correcto) repositorio.usuarioActual.first()?.let { u -> avisosSistema.programarEventos(u.materias.flatMap { it.eventos }) }
            _authUiState.value = if (correcto) {
                AuthUiState(sesionIniciada = true)
            } else {
                AuthUiState(error = R.string.error_credenciales)
            }
        }
    }

    fun registrarUsuario(nombre: String, nombreUsuario: String, contrasena: String) {
        val error = when {
            nombre.isBlank() || nombreUsuario.isBlank() || contrasena.isBlank() -> R.string.error_completar_campos
            nombreUsuario.trim().contains(' ') -> R.string.error_usuario_espacios
            contrasena.length < LARGO_MINIMO_CONTRASENA -> R.string.error_contrasena_corta
            else -> null
        }
        if (error != null) {
            _authUiState.value = AuthUiState(error = error)
            return
        }
        _authUiState.update { it.copy(procesando = true, error = null) }
        viewModelScope.launch {
            val registrado = repositorio.registrarUsuario(nombre.trim(), nombreUsuario.trim(), contrasena)
            _authUiState.value = if (registrado) {
                AuthUiState(sesionIniciada = true)
            } else {
                AuthUiState(error = R.string.error_usuario_existe)
            }
        }
    }

    // La UI ya navegó a Inicio: se limpia el estado para que no se repita.
    fun consumirSesionIniciada() {
        _authUiState.value = AuthUiState()
    }

    // Al pasar de Login a Registro (o al revés) no se arrastra el error.
    fun limpiarErrorAuth() {
        _authUiState.value = AuthUiState()
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            repositorio.cerrarSesion()
            avisosSistema.actualizarTemporizador(null)
        }
    }

    companion object {
        const val LARGO_MINIMO_CONTRASENA = 4
    }
}
