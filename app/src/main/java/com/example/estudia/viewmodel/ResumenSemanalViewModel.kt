package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.ResumenSemanal
import com.example.estudia.modelo.armarResumenSemanal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ResumenSemanalUiState(val cargando: Boolean = true, val resumen: ResumenSemanal? = null)

class ResumenSemanalViewModel(
    repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val uiState: StateFlow<ResumenSemanalUiState> = repositorio.usuarioActual
        .map { usuario -> ResumenSemanalUiState(cargando = false, resumen = usuario?.let { armarResumenSemanal(it, hoy()) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResumenSemanalUiState())
}
