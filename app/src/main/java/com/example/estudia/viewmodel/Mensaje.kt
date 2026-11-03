package com.example.estudia.viewmodel

// Mensajes que los ViewModels le piden mostrar a la UI (avisos del Snackbar
// y errores de los formularios).
//
// El ViewModel no conoce strings.xml ni la clase R de Android (apunte de
// MVVM, 3.2): solo dice QUÉ mensaje corresponde. La View lo traduce al
// texto con Mensaje.texto() (ui/componentes/TextosMensaje.kt).
enum class Mensaje {
    // Avisos del Snackbar
    AVISO_ARCHIVO_ELIMINADO,
    AVISO_ARCHIVO_ERROR,
    AVISO_ARCHIVO_GUARDADO,
    AVISO_ARCHIVO_GUARDADO_NOTA,
    AVISO_CALIFICACION_ELIMINADA,
    AVISO_EVENTO_ELIMINADO,
    AVISO_MATERIA_ARCHIVADA,
    AVISO_MATERIA_DESARCHIVADA,
    AVISO_MATERIA_ELIMINADA,
    AVISO_NOTA_ELIMINADA,
    AVISO_PERFIL_GUARDADO,
    AVISO_RECORDATORIOS_ACTIVADOS,
    AVISO_RECORDATORIOS_DESACTIVADOS,
    AVISO_SESION_COMPLETA,
    AVISO_SESION_DESCARTADA,
    AVISO_SESION_GUARDADA,
    AVISO_SESION_MUY_CORTA,
    AVISO_SIN_PERMISO_NOTIFICACIONES,
    AVISO_TAREA_ELIMINADA,

    // Errores de los formularios
    ERROR_COMPLETAR_CAMPOS,
    ERROR_CONTRASENA_CORTA,
    ERROR_CREDENCIALES,
    ERROR_HORA_FIN,
    ERROR_NOTA_INVALIDA,
    ERROR_REPETIR_HASTA,
    ERROR_TITULO_CALIFICACION,
    ERROR_USUARIO_ESPACIOS,
    ERROR_USUARIO_EXISTE
}
