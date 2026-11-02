package com.example.estudia.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

// Mensaje corto que el ViewModel le pide a la UI que muestre UNA vez en el
// Snackbar (ej: "Nota eliminada · Deshacer").
//
// Es un "evento de UI", no un estado (apunte de MVVM, cap. 13.4): por eso
// se manda por un Channel y no por un StateFlow, que se volvería a mostrar
// en cada recomposición.
//
// El texto es un id de strings.xml (@StringRes): el ViewModel no usa
// Context, así sigue sin depender de Android UI y se puede testear.
data class Aviso(
    @param:StringRes val mensaje: Int,
    val argumentos: List<Any> = emptyList(),
    // Si no es null, el Snackbar muestra el botón "Deshacer".
    val alDeshacer: (suspend () -> Unit)? = null,
    // Se ejecuta si el usuario NO tocó "Deshacer" (ej: borrar el archivo del disco).
    val alConfirmar: (suspend () -> Unit)? = null
)

// Base de los ViewModels que muestran avisos.
abstract class ViewModelConAvisos : ViewModel() {

    private val canal = Channel<Aviso>(Channel.BUFFERED)
    val avisos: Flow<Aviso> = canal.receiveAsFlow()

    protected fun avisar(aviso: Aviso) {
        canal.trySend(aviso)
    }
}
