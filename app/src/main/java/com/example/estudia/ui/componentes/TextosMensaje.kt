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
    Mensaje.AVISO_FOTO_PERFIL_GUARDADA -> R.string.aviso_foto_perfil_guardada
    Mensaje.AVISO_FOTO_PERFIL_QUITADA -> R.string.aviso_foto_perfil_quitada
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
    Mensaje.AVISO_TEMA_ELIMINADO -> R.string.aviso_tema_eliminado
    Mensaje.AVISO_TARJETA_ELIMINADA -> R.string.aviso_tarjeta_eliminada
    Mensaje.AVISO_CALENDARIO_ERROR -> R.string.aviso_calendario_error
    Mensaje.AVISO_CALENDARIO_SINCRONIZADO -> R.string.aviso_calendario_sincronizado
    Mensaje.AVISO_OCR_AGREGADO -> R.string.aviso_ocr_agregado
    Mensaje.AVISO_VERSION_RESTAURADA -> R.string.aviso_version_restaurada
    Mensaje.AVISO_GRABACION_GUARDADA -> R.string.aviso_grabacion_guardada
    Mensaje.AVISO_TAREA_REPETIDA -> R.string.aviso_tarea_repetida
    Mensaje.AVISO_RECESO_ELIMINADO -> R.string.aviso_receso_eliminado
    Mensaje.AVISO_TARJETAS_IMPORTADAS -> R.string.aviso_tarjetas_importadas
    Mensaje.AVISO_META_DIARIA_CUMPLIDA -> R.string.aviso_meta_diaria_cumplida
    Mensaje.ERROR_NOMBRE_RECESO -> R.string.error_nombre_receso
    Mensaje.ERROR_FECHAS_RECESO -> R.string.error_fechas_receso
    Mensaje.AVISO_OCR_SIN_TEXTO -> R.string.aviso_ocr_sin_texto
    Mensaje.AVISO_OCR_ERROR -> R.string.aviso_ocr_error
    Mensaje.AVISO_COPIA_GUARDADA -> R.string.aviso_copia_guardada
    Mensaje.AVISO_COPIA_RESTAURADA -> R.string.aviso_copia_restaurada
    Mensaje.AVISO_COPIA_ERROR -> R.string.aviso_copia_error
    Mensaje.AVISO_MATERIA_PLAN_ELIMINADA -> R.string.aviso_materia_plan_eliminada
    Mensaje.AVISO_PLAN_IMPORTADO -> R.string.aviso_plan_importado
    Mensaje.AVISO_EVALUACION_ELIMINADA -> R.string.aviso_evaluacion_eliminada
    Mensaje.AVISO_EVALUACIONES_IMPORTADAS -> R.string.aviso_evaluaciones_importadas
    Mensaje.AVISO_FINAL_ELIMINADO -> R.string.aviso_final_eliminado
    Mensaje.AVISO_FUENTE_ELIMINADA -> R.string.aviso_fuente_eliminada
    Mensaje.AVISO_MAPA_ELIMINADO -> R.string.aviso_mapa_eliminado
    Mensaje.AVISO_IDEA_ELIMINADA -> R.string.aviso_idea_eliminada
    Mensaje.AVISO_TARJETAS_IA_GUARDADAS -> R.string.aviso_tarjetas_ia_guardadas
    Mensaje.AVISO_NOTA_IA_GUARDADA -> R.string.aviso_nota_ia_guardada
    Mensaje.AVISO_IA_CONECTADA -> R.string.aviso_ia_conectada
    Mensaje.AVISO_IA_DESCONECTADA -> R.string.aviso_ia_desconectada
    Mensaje.AVISO_PLAN_IA_GUARDADO -> R.string.aviso_plan_ia_guardado
    Mensaje.AVISO_HORARIO_IA_GUARDADO -> R.string.aviso_horario_ia_guardado
    Mensaje.AVISO_ACCION_IA_HECHA -> R.string.aviso_accion_ia_hecha
    Mensaje.AVISO_ACCION_IA_SIN_MATERIA -> R.string.aviso_accion_ia_sin_materia
    Mensaje.AVISO_ACCION_IA_ERROR -> R.string.aviso_accion_ia_error
    Mensaje.AVISO_ACCION_IA_NO_ENCONTRADO -> R.string.aviso_accion_ia_no_encontrado
    Mensaje.AVISO_ACCION_IA_BORRADA -> R.string.aviso_accion_ia_borrada
    Mensaje.AVISO_ACCIONES_IA_HECHAS -> R.string.aviso_acciones_ia_hechas
    Mensaje.AVISO_PLAN_AL_DIA -> R.string.aviso_plan_al_dia
    Mensaje.AVISO_PLAN_REACOMODADO -> R.string.aviso_plan_reacomodado
    Mensaje.AVISO_CALENDARIO_ICS_GUARDADO -> R.string.aviso_calendario_ics_guardado
    Mensaje.AVISO_CALENDARIO_DESACTIVADO -> R.string.aviso_calendario_desactivado
    Mensaje.AVISO_MAPA_IA_CREADO -> R.string.aviso_mapa_ia_creado
    Mensaje.AVISO_COPIA_AUTO_ACTIVADA -> R.string.aviso_copia_auto_activada
    Mensaje.AVISO_COPIA_AUTO_DESACTIVADA -> R.string.aviso_copia_auto_desactivada
    Mensaje.AVISO_COPIA_AUTO_HECHA -> R.string.aviso_copia_auto_hecha
    Mensaje.AVISO_COPIA_AUTO_ERROR -> R.string.aviso_copia_auto_error
    Mensaje.AVISO_FINAL_APROBADO_PLAN -> R.string.aviso_final_aprobado_plan
    Mensaje.AVISO_PDF_ERROR -> R.string.aviso_pdf_error
    Mensaje.AVISO_FERIADOS_CALENDARIO -> R.string.aviso_feriados_calendario
    Mensaje.AVISO_FERIADOS_INTERNET -> R.string.aviso_feriados_internet
    Mensaje.AVISO_FERIADOS_ERROR -> R.string.aviso_feriados_error
    Mensaje.ERROR_COMPLETAR_CAMPOS -> R.string.error_completar_campos
    Mensaje.ERROR_CONTRASENA_CORTA -> R.string.error_contrasena_corta
    Mensaje.ERROR_CREDENCIALES -> R.string.error_credenciales
    Mensaje.ERROR_HORA_FIN -> R.string.error_hora_fin
    Mensaje.ERROR_NOTA_INVALIDA -> R.string.error_nota_invalida
    Mensaje.ERROR_REPETIR_HASTA -> R.string.error_repetir_hasta
    Mensaje.ERROR_TITULO_CALIFICACION -> R.string.error_titulo_calificacion
    Mensaje.ERROR_USUARIO_ESPACIOS -> R.string.error_usuario_espacios
    Mensaje.ERROR_USUARIO_EXISTE -> R.string.error_usuario_existe
    Mensaje.ERROR_ANIO_INVALIDO -> R.string.error_anio_invalido
    Mensaje.ERROR_NOTA_FINAL -> R.string.error_nota_final
    Mensaje.ERROR_PESO_INVALIDO -> R.string.error_peso_invalido
    Mensaje.ERROR_PESO_SUPERA -> R.string.error_peso_supera
    Mensaje.ERROR_CIERRE_INSCRIPCION -> R.string.error_cierre_inscripcion
}
