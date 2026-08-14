package com.theextramile.admin.data.api

import com.google.gson.Gson
import com.theextramile.admin.BuildConfig
import com.theextramile.admin.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP de la app. Singleton.
 *
 * IMPORTANTE: debe inicializarse desde TEMApplication.onCreate() llamando a
 * ApiClient.init(sessionManager).
 *
 * Hace dos cosas con la sesión:
 *  1. Añade "Authorization: Bearer <token>" a cada petición.
 *  2. Si el servidor responde 401, borra la sesión guardada.
 */
object ApiClient {
    val gson: Gson = Gson()

    private var sessionManager: SessionManager? = null
    private var _service: TEMApiService? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Avisa de que el servidor rechazó el token. La interfaz lo escucha para
     * explicarle al usuario por qué se le pide entrar otra vez.
     */
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    val service: TEMApiService
        get() = _service ?: throw IllegalStateException(
            "ApiClient no inicializado. Llama ApiClient.init() en Application.onCreate()"
        )

    fun init(sessionManager: SessionManager) {
        this.sessionManager = sessionManager

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = this.sessionManager?.getTokenBlocking()

            val request = if (token != null) {
                original.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }

            val response = chain.proceed(request)

            /* ── Token rechazado ──
               El backend invalida los tokens de un usuario cuando le cambian la
               contraseña (revokeUserTokens dentro de applyNewPassword, en
               usuarios.php), y también cuando caducan a los 30 días. En ambos
               casos responde 401 y la sesión guardada ya no sirve: se borra, y
               como la navegación observa currentUser, la app va sola al login y
               pide la contraseña nueva.

               Se excluye el propio login: ahí un 401 solo significa que la
               contraseña escrita está mal, no que la sesión haya caducado. */
            val esLogin = request.url.queryParameter("action") == "login"
            if (response.code == 401 && token != null && !esLogin) {
                scope.launch {
                    this@ApiClient.sessionManager?.logout()
                    _sessionExpired.tryEmit(Unit)
                }
            }

            response
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            /*
             * writeTimeout solo mide el envío del CUERPO de la petición. Para
             * un JSON no llega ni a un segundo, así que subirlo no hace que
             * nada falle más tarde; lo que permite es mandar un vídeo del
             * plan por datos móviles sin que se corte a los 30 s.
             * Conectar y leer siguen cortos: si el servidor no responde,
             * se sabe enseguida.
             */
            .writeTimeout(5, TimeUnit.MINUTES)
            .build()

        _service = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TEMApiService::class.java)
    }
}
