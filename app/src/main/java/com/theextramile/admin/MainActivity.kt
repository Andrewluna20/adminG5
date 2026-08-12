package com.theextramile.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.theextramile.admin.data.repository.ThemeRepository
import com.theextramile.admin.ui.HomeScreen
import com.theextramile.admin.ui.activity.ActivityScreen
import com.theextramile.admin.ui.activity.ActivityViewModel
import com.theextramile.admin.ui.benefits.BenefitsScreen
import com.theextramile.admin.ui.benefits.BenefitsViewModel
import com.theextramile.admin.ui.blog.BlogScreen
import com.theextramile.admin.ui.blog.BlogViewModel
import com.theextramile.admin.ui.calendar.CalendarScreen
import com.theextramile.admin.ui.calendar.CalendarViewModel
import com.theextramile.admin.ui.extracto.ExtractoScreen
import com.theextramile.admin.ui.extracto.ExtractoViewModel
import com.theextramile.admin.ui.gcal.GoogleCalendarScreen
import com.theextramile.admin.ui.gcal.GoogleCalendarViewModel
import com.theextramile.admin.ui.login.LoginScreen
import com.theextramile.admin.ui.login.LoginViewModel
import com.theextramile.admin.ui.overview.OverviewScreen
import com.theextramile.admin.ui.overview.OverviewViewModel
import com.theextramile.admin.ui.planconfig.PlanConfigScreen
import com.theextramile.admin.ui.planconfig.PlanConfigViewModel
import com.theextramile.admin.ui.reservations.ReservationsScreen
import com.theextramile.admin.ui.reservations.ReservationsViewModel
import com.theextramile.admin.ui.seo.SeoScreen
import com.theextramile.admin.ui.seo.SeoViewModel
import com.theextramile.admin.ui.settings.SettingsScreen
import com.theextramile.admin.ui.settings.SettingsViewModel
import com.theextramile.admin.ui.theme.TEMAdminTheme
import com.theextramile.admin.ui.tours.ToursScreen
import com.theextramile.admin.ui.tours.ToursViewModel
import com.theextramile.admin.ui.users.UsersScreen
import com.theextramile.admin.ui.users.UsersViewModel
import com.theextramile.admin.util.Roles
import com.theextramile.admin.util.Section
import kotlinx.coroutines.launch

/**
 * Punto de entrada de Admin G.
 *
 * 1) Carga el tema remoto desde el HUB
 * 2) Sin sesión → LoginScreen
 * 3) Con sesión → HomeScreen con las 13 secciones del panel web, filtradas
 *    por el rol del usuario (ver util/Permissions.kt)
 */
class MainActivity : ComponentActivity() {

    private lateinit var themeRepo: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themeRepo = ThemeRepository(this)
        lifecycleScope.launch { themeRepo.loadTheme() }

        val app = application as TEMApplication

        setContent {
            val remoteTheme by themeRepo.theme.collectAsState()
            TEMAdminTheme(remoteTheme = remoteTheme) {
                AppNavigation(app)
            }
        }
    }
}

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"

    /** Cada sección del panel usa su propia clave como ruta */
    fun of(section: Section) = section.key
}

@Composable
fun AppNavigation(app: TEMApplication) {
    val navController = rememberNavController()
    val sessionManager = app.sessionManager
    val currentUser by sessionManager.currentUser.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var initialChecked by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf(Routes.LOGIN) }

    LaunchedEffect(Unit) {
        startDestination = if (sessionManager.isLoggedIn()) Routes.HOME else Routes.LOGIN
        initialChecked = true
    }

    if (!initialChecked) {
        LoadingScreen()
        return
    }

    val role = currentUser?.role
    val siteBaseUrl = BuildConfig.SITE_BASE_URL
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(
                factory = SimpleViewModelFactory { LoginViewModel(app.authRepository) }
            )
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val user = currentUser
            if (user == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
                return@composable
            }
            HomeScreen(
                user = user,
                onLogout = {
                    scope.launch {
                        sessionManager.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                },
                onNavigate = { section -> navController.navigate(Routes.of(section)) }
            )
        }

        // ═══════ General ═══════

        composable(Routes.of(Section.OVERVIEW)) {
            Guarded(
                section = Section.OVERVIEW,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: OverviewViewModel = viewModel(
                    factory = SimpleViewModelFactory { OverviewViewModel(app.reservationRepository) }
                )
                OverviewScreen(
                    viewModel = vm,
                    userName = currentUser?.name.orEmpty(),
                    onBack = back,
                    onOpenReservations = { navController.navigate(Routes.of(Section.RESERVATIONS)) }
                )
            }
        }

        composable(Routes.of(Section.RESERVATIONS)) {
            Guarded(
                section = Section.RESERVATIONS,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: ReservationsViewModel = viewModel(
                    factory = SimpleViewModelFactory { ReservationsViewModel(app.reservationRepository) }
                )
                ReservationsScreen(viewModel = vm, onBack = back)
            }
        }

        composable(Routes.of(Section.CALENDAR)) {
            Guarded(
                section = Section.CALENDAR,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: CalendarViewModel = viewModel(
                    factory = SimpleViewModelFactory { CalendarViewModel(app.reservationRepository) }
                )
                CalendarScreen(
                    viewModel = vm,
                    canManage = Roles.canManageReservations(role),
                    onBack = back
                )
            }
        }

        // ═══════ Gestión del sitio ═══════

        composable(Routes.of(Section.TOURS)) {
            Guarded(
                section = Section.TOURS,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: ToursViewModel = viewModel(
                    factory = SimpleViewModelFactory { ToursViewModel(app.tourRepository) }
                )
                ToursScreen(viewModel = vm, onBack = back)
            }
        }

        composable(Routes.of(Section.PLAN_CONFIG)) {
            Guarded(
                section = Section.PLAN_CONFIG,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: PlanConfigViewModel = viewModel(
                    factory = SimpleViewModelFactory {
                        PlanConfigViewModel(
                            app.planConfigRepository,
                            app.tourRepository,
                            app.uploadRepository
                        )
                    }
                )
                PlanConfigScreen(viewModel = vm, siteBaseUrl = siteBaseUrl, onBack = back)
            }
        }

        composable(Routes.of(Section.BENEFITS)) {
            Guarded(
                section = Section.BENEFITS,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: BenefitsViewModel = viewModel(
                    factory = SimpleViewModelFactory {
                        BenefitsViewModel(app.benefitRepository, app.uploadRepository)
                    }
                )
                BenefitsScreen(viewModel = vm, siteBaseUrl = siteBaseUrl, onBack = back)
            }
        }

        composable(Routes.of(Section.BLOG)) {
            Guarded(
                section = Section.BLOG,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: BlogViewModel = viewModel(
                    factory = SimpleViewModelFactory {
                        BlogViewModel(app.blogRepository, app.uploadRepository)
                    }
                )
                BlogScreen(viewModel = vm, siteBaseUrl = siteBaseUrl, onBack = back)
            }
        }

        composable(Routes.of(Section.SEO)) {
            Guarded(
                section = Section.SEO,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: SeoViewModel = viewModel(
                    factory = SimpleViewModelFactory {
                        SeoViewModel(app.settingsRepository, app.uploadRepository)
                    }
                )
                SeoScreen(viewModel = vm, siteBaseUrl = siteBaseUrl, onBack = back)
            }
        }

        composable(Routes.of(Section.EXTRACTO)) {
            Guarded(
                section = Section.EXTRACTO,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: ExtractoViewModel = viewModel(
                    factory = SimpleViewModelFactory {
                        ExtractoViewModel(
                            app.reservationRepository,
                            app.tourRepository,
                            app.planConfigRepository
                        )
                    }
                )
                ExtractoScreen(viewModel = vm, onBack = back)
            }
        }

        composable(Routes.of(Section.ACTIVITY)) {
            Guarded(
                section = Section.ACTIVITY,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: ActivityViewModel = viewModel(
                    factory = SimpleViewModelFactory { ActivityViewModel(app.activityRepository) }
                )
                ActivityScreen(viewModel = vm, onBack = back)
            }
        }

        composable(Routes.of(Section.SETTINGS)) {
            Guarded(
                section = Section.SETTINGS,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: SettingsViewModel = viewModel(
                    factory = SimpleViewModelFactory {
                        SettingsViewModel(app.settingsRepository, app.uploadRepository)
                    }
                )
                SettingsScreen(viewModel = vm, siteBaseUrl = siteBaseUrl, onBack = back)
            }
        }

        // ═══════ Usuarios ═══════

        composable(Routes.of(Section.USERS)) {
            Guarded(
                section = Section.USERS,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val user = currentUser ?: return@Guarded
                val vm: UsersViewModel = viewModel(
                    factory = SimpleViewModelFactory { UsersViewModel(app.userRepository) }
                )
                UsersScreen(viewModel = vm, currentUserId = user.id, onBack = back)
            }
        }

        composable(Routes.of(Section.GCAL)) {
            Guarded(
                section = Section.GCAL,
                role = role,
                isLoggedIn = currentUser != null,
                onDenied = { navController.popBackStack() },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            ) {
                val vm: GoogleCalendarViewModel = viewModel(
                    factory = SimpleViewModelFactory { GoogleCalendarViewModel(app.gcalRepository) }
                )
                GoogleCalendarScreen(viewModel = vm, onBack = back)
            }
        }
    }
}

/**
 * Guard de permisos — el equivalente de navAllowedFor() en core.js.
 *
 * Si el rol no puede ver la sección, se vuelve atrás en vez de abrirla. Esto
 * es comodidad, no seguridad: el backend vuelve a comprobar el permiso en
 * cada endpoint con requirePanelAdmin().
 */
@Composable
private fun Guarded(
    section: Section,
    role: String?,
    isLoggedIn: Boolean,
    onDenied: () -> Unit,
    onNoSession: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!isLoggedIn) {
        LaunchedEffect(Unit) { onNoSession() }
        return
    }
    if (!Roles.canSee(role, section)) {
        LaunchedEffect(section) { onDenied() }
        return
    }
    content()
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050810), Color(0xFF0A1738), Color(0xFF050810))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("The Extra ", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Text(
                "Mile",
                color = Color(0xFFC9A84C),
                fontSize = 36.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Color(0xFFC9A84C))
        }
    }
}
