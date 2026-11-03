package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Mensaje corto que el ViewModel le pide a la UI que muestre UNA vez en el
// Snackbar (ej: "Nota eliminada · Deshacer").
//
// Es un "evento de UI", no un estado (apunte de MVVM, cap. 13.4): por eso
// se manda por un SharedFlow y no por un StateFlow, que se volvería a
// mostrar en cada recomposición.
//
// El texto es un Mensaje (enum propio): el ViewModel no usa Context ni R,
// así no depende de Android y se puede testear.
data class Aviso(
    val mensaje: Mensaje,
    val argumentos: List<Any> = emptyList(),
    // Si no es null, el Snackbar muestra el botón "Deshacer".
    val alDeshacer: (suspend () -> Unit)? = null,
    // Se ejecuta si el usuario NO tocó "Deshacer" (ej: borrar el archivo del disco).
    val alConfirmar: (suspend () -> Unit)? = null
)

// Base de los ViewModels que muestran avisos.
abstract class ViewModelConAvisos : ViewModel() {

    private val _avisos = MutableSharedFlow<Aviso>()
    val avisos: SharedFlow<Aviso> = _avisos.asSharedFlow()

    // Un SharedFlow descarta lo que se emite cuando nadie lo escucha (por
    // ejemplo, en medio de una rotación de pantalla). Para no perder el
    // "Deshacer", se espera a que la pantalla vuelva a escuchar.
    protected fun avisar(aviso: Aviso) {
        viewModelScope.launch {
            _avisos.subscriptionCount.first { it > 0 }
            _avisos.emit(aviso)
        }
    }
}
