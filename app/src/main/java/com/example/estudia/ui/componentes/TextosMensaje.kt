package com.example.estudia.ui.componentes

import androidx.annotation.StringRes
import com.example.estudia.R
import com.example.estudia.viewmodel.Mensaje

// Traduce cada Mensaje del ViewModel a su texto de strings.xml.
// Es la única parte que conoce R: los ViewModels quedan sin dependencias
// de Android.
@StringRes
fun Mensaje.texto(): Int = when (this) {
    Mensaje.AVISO_ARCHIVO_ELIMINADO -> R.string.aviso_archivo_eliminado
    Mensaje.AVISO_ARCHIVO_ERROR -> R.string.aviso_archivo_error
    Mensaje.AVISO_ARCHIVO_GUARDADO -> R.string.aviso_archivo_guardado
    Mensaje.AVISO_ARCHIVO_GUARDADO_NOTA -> R.string.aviso_archivo_guardado_nota
    Mensaje.AVISO_CALIFICACION_ELIMINADA -> R.string.aviso_calificacion_eliminada
    Mensaje.AVISO_EVENTO_ELIMINADO -> R.string.aviso_evento_eliminado
    Mensaje.AVISO_MATERIA_ARCHIVADA -> R.string.aviso_materia_archivada
    Mensaje.AVISO_MATERIA_DESARCHIVADA -> R.string.aviso_materia_desarchivada
    Mensaje.AVISO_MATERIA_ELIMINADA -> R.string.aviso_materia_eliminada
    Mensaje.AVISO_NOTA_ELIMINADA -> R.string.aviso_nota_eliminada
    Mensaje.AVISO_PERFIL_GUARDADO -> R.string.aviso_perfil_guardado
    Mensaje.AVISO_RECORDATORIOS_ACTIVADOS -> R.string.aviso_recordatorios_activados
    Mensaje.AVISO_RECORDATORIOS_DESACTIVADOS -> R.string.aviso_recordatorios_desactivados
    Mensaje.AVISO_SESION_COMPLETA -> R.string.aviso_sesion_completa
    Mensaje.AVISO_SESION_DESCARTADA -> R.string.aviso_sesion_descartada
    Mensaje.AVISO_SESION_GUARDADA -> R.string.aviso_sesion_guardada
    Mensaje.AVISO_SESION_MUY_CORTA -> R.string.aviso_sesion_muy_corta
    Mensaje.AVISO_SIN_PERMISO_NOTIFICACIONES -> R.string.aviso_sin_permiso_notificaciones
    Mensaje.AVISO_TAREA_ELIMINADA -> R.string.aviso_tarea_eliminada
    Mensaje.ERROR_COMPLETAR_CAMPOS -> R.string.error_completar_campos
    Mensaje.ERROR_CONTRASENA_CORTA -> R.string.error_contrasena_corta
    Mensaje.ERROR_CREDENCIALES -> R.string.error_credenciales
    Mensaje.ERROR_HORA_FIN -> R.string.error_hora_fin
    Mensaje.ERROR_NOTA_INVALIDA -> R.string.error_nota_invalida
    Mensaje.ERROR_REPETIR_HASTA -> R.string.error_repetir_hasta
    Mensaje.ERROR_TITULO_CALIFICACION -> R.string.error_titulo_calificacion
    Mensaje.ERROR_USUARIO_ESPACIOS -> R.string.error_usuario_espacios
    Mensaje.ERROR_USUARIO_EXISTE -> R.string.error_usuario_existe
}
