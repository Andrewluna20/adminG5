package com.theextramile.admin.data.api

import com.google.gson.Gson
import com.theextramile.admin.BuildConfig
import com.theextramile.admin.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP de la app. Singleton.
 *
 * IMPORTANTE: debe inicializarse desde TEMApplication.onCreate() llamando
 * a ApiClient.init(sessionManager).
 *
 * El AuthInterceptor añade automáticamente el header
 * "Authorization: Bearer <token>" en cada petición si hay sesión activa.
 */
object ApiClient {
    val gson: Gson = Gson()

    private var sessionManager: SessionManager? = null
    private var _service: TEMApiService? = null

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
            chain.proceed(request)
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
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        _service = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TEMApiService::class.java)
    }
}
