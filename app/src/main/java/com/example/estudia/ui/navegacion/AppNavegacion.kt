package com.example.estudia.ui.navegacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.estudia.ui.componentes.AccionRapida
import com.example.estudia.ui.componentes.abrirGemini
import com.example.estudia.ui.componentes.LocalFondoLiquido
import com.example.estudia.ui.componentes.guardarFondoLiquido
import com.example.estudia.ui.componentes.rememberFondoLiquido
import com.example.estudia.ui.componentes.rememberLuzDelTelefono
import com.example.estudia.ui.componentes.deslizarEntreSecciones
import com.example.estudia.ui.componentes.LocalCambiarSeccion
import com.example.estudia.ui.componentes.LocalTransicionCompartida
import com.example.estudia.ui.componentes.LocalAlcanceAnimado
import com.example.estudia.ui.pantallas.NotaDetalleScreen
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.example.estudia.ui.componentes.seccionVecina
import com.example.estudia.ui.componentes.LocalLuz
import androidx.compose.ui.graphics.graphicsLayer
import com.example.estudia.R
import kotlinx.coroutines.launch
import com.example.estudia.ui.componentes.DockFlotante
import com.example.estudia.ui.componentes.LocalHaze
import com.example.estudia.ui.componentes.buscaEnLaPantalla
import com.example.estudia.ui.componentes.fondoAurora
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.example.estudia.ui.componentes.LocalEspacioDock
import com.example.estudia.ui.componentes.ControladorAvisos
import com.example.estudia.ui.componentes.LocalAvisos
import com.example.estudia.ui.pantallas.AyudaScreen
import com.example.estudia.ui.pantallas.BienvenidaScreen
import com.example.estudia.ui.pantallas.CalendarioScreen
import com.example.estudia.ui.pantallas.EstadisticasScreen
import com.example.estudia.ui.pantallas.HorarioScreen
import com.example.estudia.ui.pantallas.LogrosScreen
import com.example.estudia.ui.pantallas.RepasoScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.estudia.ui.pantallas.InicioScreen
import com.example.estudia.ui.pantallas.LoginScreen
import com.example.estudia.ui.pantallas.MateriasScreen
import com.example.estudia.ui.pantallas.NotasScreen
import com.example.estudia.ui.pantallas.PerfilScreen
import com.example.estudia.ui.pantallas.RegistroScreen
import com.example.estudia.ui.pantallas.TemporizadorScreen
import com.example.estudia.ui.pantallas.AsistenciaScreen
import com.example.estudia.ui.pantallas.MatrizScreen
import com.example.estudia.ui.pantallas.BusquedaScreen
import com.example.estudia.ui.pantallas.CarreraScreen
import com.example.estudia.ui.pantallas.FinalesScreen
import com.example.estudia.ui.pantallas.BibliografiaScreen
import com.example.estudia.ui.pantallas.MapasScreen
import com.example.estudia.ui.pantallas.AsistenteScreen
import com.example.estudia.ui.pantallas.GuiaPrimeraVez
import com.example.estudia.ui.pantallas.PasoGuia
import com.example.estudia.ui.pantallas.MapaEditorScreen
import com.example.estudia.ui.pantallas.NotaNecesariaScreen
import com.example.estudia.ui.pantallas.QuizScreen
import com.example.estudia.ui.pantallas.ResumenSemanalScreen
import com.example.estudia.viewmodel.TipoResultado
import com.example.estudia.viewmodel.AppViewModelProvider
import com.example.estudia.viewmodel.SesionUiState
import com.example.estudia.viewmodel.SesionViewModel
import com.example.estudia.viewmodel.TODAS_LAS_MATERIAS

// Grafo de navegación de la app: rutas, barra inferior y el Snackbar.
// Cada pantalla pide su propio ViewModel con viewModel(factory = ...):
// queda asociado a esa entrada de la navegación y se destruye al salir.
// La sesión la maneja SesionViewModel, que vive mientras vive la Activity.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavegacion(
    sesionViewModel: SesionViewModel,
    // Lo que se pidió desde un widget o la notificación del temporizador
    // (ver MainActivity y AccionExterna).
    accionExterna: AccionExterna? = null,
    onAccionHecha: () -> Unit = {}
) {
    val sesionUiState by sesionViewModel.sesionUiState.collectAsStateWithLifecycle()
    val authUiState by sesionViewModel.authUiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val mostrarBarra = pantallasConBarra.any { it.ruta == rutaActual }

    val snackbarHostState = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val contexto = LocalContext.current
    val recursos = contexto.resources
    val avisos = remember { ControladorAvisos(snackbarHostState, alcance, recursos) }

    // Lo que pidió el botón "+" del dock: la pantalla en la que se está abre
    // su diálogo de creación y después lo limpia.
    var accionPendiente by rememberSaveable { mutableStateOf<AccionRapida?>(null) }

    // Búsqueda desde la lupa del dock (en Materias, Calendario y Notas filtra la lista).
    // Se cierra y se borra al cambiar de pantalla.
    var buscando by rememberSaveable { mutableStateOf(false) }
    var textoBusqueda by rememberSaveable { mutableStateOf("") }
    // (Se guarda la ruta para no borrar la búsqueda al girar el teléfono.)
    var rutaDeLaBusqueda by rememberSaveable { mutableStateOf(rutaActual) }
    LaunchedEffect(rutaActual) {
        if (rutaActual != rutaDeLaBusqueda) {
            buscando = false
            textoBusqueda = ""
            rutaDeLaBusqueda = rutaActual
        }
    }

    // Qué se desenfoca detrás del dock de vidrio: el fondo y las pantallas.
    val haze = remember { HazeState() }
    val irA: (Screen) -> Unit = { destino ->
        navController.navigate(destino.ruta) {
            popUpTo(Screen.Inicio.ruta) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Mientras se lee la sesión guardada, se muestra un indicador de carga.
    if (sesionUiState is SesionUiState.Cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Se decide una sola vez (remember): si había sesión guardada se arranca
    // directo en Inicio; si no, en Bienvenida.
    val destinoInicial = remember {
        if (sesionUiState is SesionUiState.Activa) Screen.Inicio.ruta else Screen.Bienvenida.ruta
    }

    // Cuando el login o el registro salen bien, se navega a Inicio una vez.
    LaunchedEffect(authUiState.sesionIniciada) {
        if (authUiState.sesionIniciada) {
            navController.navigate(Screen.Inicio.ruta) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
            sesionViewModel.consumirSesionIniciada()
        }
    }

    // Un botón de un widget o la notificación del temporizador: se hace la
    // acción (si hay alguien con sesión iniciada). Las de crear abren la
    // sección y su diálogo, como el botón + del dock.
    LaunchedEffect(accionExterna, sesionUiState) {
        val accion = accionExterna ?: return@LaunchedEffect
        if (sesionUiState is SesionUiState.Activa) {
            // Recién abierta la app, se espera a que la navegación esté lista.
            navController.currentBackStackEntryFlow.first()
            when (accion) {
                AccionExterna.TEMPORIZADOR -> navController.navigate(Screen.Temporizador.ruta) { launchSingleTop = true }
                AccionExterna.NUEVA_NOTA -> { accionPendiente = AccionRapida.NOTA; irA(Screen.Notas) }
                AccionExterna.NUEVO_EVENTO -> { accionPendiente = AccionRapida.EVENTO; irA(Screen.Calendario) }
                AccionExterna.NUEVA_MATERIA -> { accionPendiente = AccionRapida.MATERIA; irA(Screen.Materias) }
                AccionExterna.ASISTENTE -> irA(Screen.Asistente)
                AccionExterna.HORARIO -> navController.navigate(Screen.Horario.ruta) { launchSingleTop = true }
                AccionExterna.CALENDARIO -> irA(Screen.Calendario)
                AccionExterna.MATERIAS -> irA(Screen.Materias)
                AccionExterna.NOTAS -> irA(Screen.Notas)
                AccionExterna.BUSCAR -> navController.navigate(Screen.Busqueda.ruta) { launchSingleTop = true }
            }
        }
        onAccionHecha()
    }

    // Guía de primera vez (usuario nuevo, sin materias). Sus botones cierran la
    // guía y llevan a cargar materias, al horario desde una foto o a Perfil.
    val mostrarGuia by sesionViewModel.mostrarGuia.collectAsStateWithLifecycle()
    var abrirHorarioIA by rememberSaveable { mutableStateOf(false) }
    if (mostrarGuia && mostrarBarra) {
        GuiaPrimeraVez(
            onPaso = { paso ->
                sesionViewModel.cerrarGuia()
                when (paso) {
                    PasoGuia.MATERIAS -> { accionPendiente = AccionRapida.MATERIA; irA(Screen.Materias) }
                    PasoGuia.HORARIO -> { abrirHorarioIA = true; irA(Screen.Asistente) }
                    PasoGuia.RECORDATORIOS -> irA(Screen.Perfil)
                }
            },
            onCerrar = sesionViewModel::cerrarGuia
        )
    }

    // La pantalla guardada para el vidrio líquido del dock (Android 13 o más).
    val fondoLiquido = rememberFondoLiquido()
    // De dónde viene la luz del vidrio (se corre al inclinar el teléfono).
    val luz = rememberLuzDelTelefono()
    CompositionLocalProvider(LocalAvisos provides avisos, LocalHaze provides haze, LocalFondoLiquido provides fondoLiquido, LocalLuz provides luz) {
        Scaffold(
            // Transparente: el fondo con manchas de color lo pinta el contenido
            // (así también se desenfoca detrás del dock).
            containerColor = Color.Transparent,
            // Con fondo transparente Material no sabe de qué color va el texto
            // (quedaba negro en el tema oscuro): se le indica el del tema.
            contentColor = MaterialTheme.colorScheme.onBackground,
            // Único Scaffold de la app (como en la clase): muestra los avisos de
            // todas las pantallas.
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                // El dock sube desde abajo al volver a una sección y baja al
                // abrir una pantalla sin dock (Temporizador, Estadísticas...).
                AnimatedVisibility(
                    visible = mostrarBarra,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                    exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200))
                ) {
                    DockFlotante(
                        rutaActual = rutaActual,
                        buscando = buscando,
                        textoBusqueda = textoBusqueda,
                        onSeleccionar = irA,
                        onCrear = { accion ->
                            when (accion) {
                                AccionRapida.ESTUDIAR -> navController.navigate(Screen.Temporizador.ruta)
                                AccionRapida.GEMINI -> if (!abrirGemini(contexto)) {
                                    alcance.launch { snackbarHostState.showSnackbar(recursos.getString(R.string.gemini_no_se_pudo_abrir)) }
                                }
                                else -> accionPendiente = accion
                            }
                        },
                        onBuscar = {
                            if (buscaEnLaPantalla(rutaActual)) buscando = true
                            else navController.navigate(Screen.Busqueda.ruta)
                        },
                        onCambiarBusqueda = { textoBusqueda = it },
                        onCerrarBusqueda = {
                            buscando = false
                            textoBusqueda = ""
                        }
                    )
                }
            }
        ) { espacioInterno ->
          // El contenido pasa por detrás del dock flotante: solo se respeta el
          // espacio de arriba y las listas suman LocalEspacioDock abajo.
          // Deslizando a la izquierda o a la derecha se pasa a la sección de al
          // lado del dock (no mientras se busca).
          val navegarDeslizando = mostrarBarra && !buscando
          // Siempre la misma función (llama a la versión más nueva): si cambiara,
          // se volverían a armar todas las pantallas que la reciben.
          val cambiarActual by rememberUpdatedState<(Int) -> Unit> { paso -> seccionVecina(rutaActual, paso)?.let(irA) }
          val cambiarSeccion: (Int) -> Unit = remember { { paso -> cambiarActual(paso) } }
          CompositionLocalProvider(
            LocalEspacioDock provides if (mostrarBarra) espacioInterno.calculateBottomPadding() else 0.dp,
            LocalCambiarSeccion provides if (navegarDeslizando) cambiarSeccion else null
          ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .deslizarEntreSecciones(navegarDeslizando, cambiarSeccion)
              .guardarFondoLiquido(fondoLiquido)
              .hazeSource(haze)
          ) {
            // El fondo va en su propia capa: así, cuando se mueve, no se
            // redibuja toda la pantalla.
            Box(modifier = Modifier.matchParentSize().graphicsLayer().fondoAurora())
            // Para las transiciones compartidas (la tarjeta de una nota que se
            // agranda hasta ser la nota abierta): ver TransicionCompartida.kt.
            SharedTransitionLayout {
            CompositionLocalProvider(LocalTransicionCompartida provides this) {
            NavHost(
                navController = navController,
                startDestination = destinoInicial,
                // Las pantallas con barra inferior respetan el espacio del Scaffold;
                // las demás (Bienvenida, Login, Registro, Ayuda, Temporizador, Estadísticas) dibujan
                // borde y manejan ellas mismas las barras del sistema.
                modifier = if (mostrarBarra) Modifier.padding(top = espacioInterno.calculateTopPadding()) else Modifier,
                // Transiciones entre pantallas: ver Transiciones.kt.
                enterTransition = { entrar() },
                exitTransition = { salir() },
                popEnterTransition = { volverAEntrar() },
                popExitTransition = { volverASalir() }
            ) {
                composable(Screen.Bienvenida.ruta) {
                    BienvenidaScreen(onComenzar = { navController.navigate(Screen.Login.ruta) })
                }

                composable(Screen.Login.ruta) {
                    LoginScreen(
                        estado = authUiState,
                        onIniciarSesion = sesionViewModel::iniciarSesion,
                        onIrARegistro = {
                            sesionViewModel.limpiarErrorAuth()
                            navController.navigate(Screen.Registro.ruta)
                        }
                    )
                }

                composable(Screen.Registro.ruta) {
                    RegistroScreen(
                        estado = authUiState,
                        onRegistrar = sesionViewModel::registrarUsuario,
                        onVolverALogin = {
                            sesionViewModel.limpiarErrorAuth()
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Inicio.ruta) {
                    InicioScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onAbrirPerfil = { irA(Screen.Perfil) },
                        onAbrirRacha = { navController.navigate(Screen.Estadisticas.ruta) },
                        onRepasar = { navController.navigate(Screen.Repaso.de(TODAS_LAS_MATERIAS)) }
                    )
                }

                composable(Screen.Materias.ruta) {
                    MateriasScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        crearAlAbrir = accionPendiente == AccionRapida.MATERIA,
                        busqueda = textoBusqueda,
                        onRepasar = { navController.navigate(Screen.Repaso.de(it)) },
                        onQuiz = { navController.navigate(Screen.Quiz.de(it)) },
                        onPares = { navController.navigate(Screen.Pares.de(it)) },
                        onNotaNecesaria = { navController.navigate(Screen.NotaNecesaria.de(it)) },
                        onAbrirCarrera = { navController.navigate(Screen.Carrera.ruta) },
                        onAbrirFinales = { navController.navigate(Screen.Finales.ruta) },
                        onAbrirMatriz = { navController.navigate(Screen.Matriz.ruta) },
                        onAbrirBibliografia = { navController.navigate(Screen.Bibliografia.ruta) },
                        onCreacionAbierta = { accionPendiente = null }
                    )
                }

                composable(Screen.Calendario.ruta) {
                    CalendarioScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        crearAlAbrir = accionPendiente == AccionRapida.EVENTO,
                        busqueda = textoBusqueda,
                        onCerrarBusqueda = {
                            buscando = false
                            textoBusqueda = ""
                        },
                        onCreacionAbierta = { accionPendiente = null },
                        onAbrirHorario = { navController.navigate(Screen.Horario.ruta) },
                        onAbrirAsistencia = { navController.navigate(Screen.Asistencia.ruta) }
                    )
                }

                composable(Screen.Asistente.ruta) {
                    AsistenteScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onAbrirMapa = { navController.navigate(Screen.Mapa.de(it)) },
                        abrirHorario = abrirHorarioIA,
                        onHorarioAbierto = { abrirHorarioIA = false }
                    )
                }

                composable(Screen.Notas.ruta) {
                    CompositionLocalProvider(LocalAlcanceAnimado provides this) {
                        NotasScreen(
                            viewModel = viewModel(factory = AppViewModelProvider.Factory),
                            crearAlAbrir = accionPendiente == AccionRapida.NOTA,
                            busqueda = textoBusqueda,
                            onCreacionAbierta = { accionPendiente = null },
                            onAbrirMapas = { navController.navigate(Screen.Mapas.ruta) },
                            onAbrirNota = { navController.navigate(Screen.Nota.de(it)) }
                        )
                    }
                }

                // La nota abierta usa el ViewModel de la lista de notas (el de
                // su pantalla en la pila), así los cambios se ven en las dos.
                composable(Screen.Nota.ruta, arguments = listOf(navArgument(Screen.Nota.ARGUMENTO) { type = NavType.IntType })) { entrada ->
                    val dueño = remember(entrada) { runCatching { navController.getBackStackEntry(Screen.Notas.ruta) }.getOrNull() ?: entrada }
                    CompositionLocalProvider(LocalAlcanceAnimado provides this) {
                        NotaDetalleScreen(
                            notaId = entrada.arguments?.getInt(Screen.Nota.ARGUMENTO) ?: -1,
                            viewModel = viewModel(viewModelStoreOwner = dueño, factory = AppViewModelProvider.Factory),
                            // Solo si sigue abierta: puede pedirse dos veces (al borrarla).
                            onVolver = { if (navController.currentDestination?.route == Screen.Nota.ruta) navController.popBackStack() }
                        )
                    }
                }

                composable(Screen.Perfil.ruta) {
                    PerfilScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onAbrirAyuda = { navController.navigate(Screen.Ayuda.ruta) },
                        onAbrirEstadisticas = { navController.navigate(Screen.Estadisticas.ruta) },
                        onAbrirLogros = { navController.navigate(Screen.Logros.ruta) },
                        onCerrarSesion = {
                            sesionViewModel.cerrarSesion()
                            navController.navigate(Screen.Login.ruta) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Estadisticas.ruta) {
                    EstadisticasScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onVolver = { navController.popBackStack() },
                        onAbrirSemana = { navController.navigate(Screen.ResumenSemanal.ruta) }
                    )
                }

                composable(Screen.Horario.ruta) {
                    HorarioScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onVolver = { navController.popBackStack() }
                    )
                }

                composable(
                    Screen.Repaso.ruta,
                    arguments = listOf(navArgument(Screen.Repaso.ARGUMENTO) { type = NavType.IntType })
                ) {
                    RepasoScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onVolver = { navController.popBackStack() }
                    )
                }

                composable(Screen.Logros.ruta) {
                    LogrosScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onVolver = { navController.popBackStack() }
                    )
                }

                composable(Screen.Temporizador.ruta) {
                    TemporizadorScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onVolver = { navController.popBackStack() }
                    )
                }

                composable(Screen.Ayuda.ruta) {
                    AyudaScreen(onVolver = { navController.popBackStack() })
                }

                composable(Screen.Carrera.ruta) {
                    CarreraScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }

                composable(Screen.Asistencia.ruta) {
                    AsistenciaScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }
                composable(Screen.Matriz.ruta) {
                    MatrizScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }

                composable(Screen.Finales.ruta) {
                    FinalesScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }

                composable(Screen.Bibliografia.ruta) {
                    BibliografiaScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }

                composable(Screen.Mapas.ruta) {
                    MapasScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onVolver = { navController.popBackStack() },
                        onAbrir = { navController.navigate(Screen.Mapa.de(it)) }
                    )
                }

                composable(Screen.Mapa.ruta, arguments = listOf(navArgument("mapaId") { type = NavType.IntType })) {
                    MapaEditorScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }

                composable(Screen.ResumenSemanal.ruta) {
                    ResumenSemanalScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }

                composable(Screen.Busqueda.ruta) {
                    BusquedaScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onVolver = { navController.popBackStack() },
                        // Cada resultado lleva a la sección donde está.
                        onAbrir = { tipo ->
                            when (tipo) {
                                TipoResultado.MATERIA, TipoResultado.TAREA, TipoResultado.TARJETA -> irA(Screen.Materias)
                                TipoResultado.NOTA -> irA(Screen.Notas)
                                TipoResultado.EVENTO -> irA(Screen.Calendario)
                                TipoResultado.CARRERA -> navController.navigate(Screen.Carrera.ruta)
                                TipoResultado.FINAL -> navController.navigate(Screen.Finales.ruta)
                                TipoResultado.FUENTE -> navController.navigate(Screen.Bibliografia.ruta)
                                TipoResultado.MAPA -> navController.navigate(Screen.Mapas.ruta)
                            }
                        }
                    )
                }

                composable(
                    Screen.NotaNecesaria.ruta,
                    arguments = listOf(navArgument(Screen.Repaso.ARGUMENTO) { type = NavType.IntType })
                ) {
                    NotaNecesariaScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }

                composable(
                    Screen.Quiz.ruta,
                    arguments = listOf(navArgument(Screen.Repaso.ARGUMENTO) { type = NavType.IntType })
                ) {
                    QuizScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }

                composable(
                    Screen.Pares.ruta,
                    arguments = listOf(navArgument(Screen.Repaso.ARGUMENTO) { type = NavType.IntType })
                ) {
                    com.example.estudia.ui.pantallas.ParesScreen(viewModel = viewModel(factory = AppViewModelProvider.Factory), onVolver = { navController.popBackStack() })
                }
            }
            }
            }
          }
          }
        }

    }
}
