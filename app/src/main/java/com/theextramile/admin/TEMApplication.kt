package com.theextramile.admin

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.local.SessionManager
import com.theextramile.admin.data.repository.ActivityRepository
import com.theextramile.admin.data.repository.AuthRepository
import com.theextramile.admin.data.repository.BenefitRepository
import com.theextramile.admin.data.repository.BlogRepository
import com.theextramile.admin.data.repository.GoogleCalendarRepository
import com.theextramile.admin.data.repository.NotificationRepository
import com.theextramile.admin.data.repository.PlanConfigRepository
import com.theextramile.admin.data.repository.ReservationRepository
import com.theextramile.admin.data.repository.SettingsRepository
import com.theextramile.admin.data.repository.TourRepository
import com.theextramile.admin.data.repository.UploadRepository
import com.theextramile.admin.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TEMApplication : Application() {
    lateinit var sessionManager: SessionManager
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var reservationRepository: ReservationRepository
        private set
    lateinit var tourRepository: TourRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var userRepository: UserRepository
        private set
    lateinit var gcalRepository: GoogleCalendarRepository
        private set
    lateinit var notificationRepository: NotificationRepository
        private set

    // ── Secciones portadas del panel web ──
    lateinit var benefitRepository: BenefitRepository
        private set
    lateinit var blogRepository: BlogRepository
        private set
    lateinit var planConfigRepository: PlanConfigRepository
        private set
    lateinit var activityRepository: ActivityRepository
        private set
    lateinit var uploadRepository: UploadRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
        ApiClient.init(sessionManager)
        authRepository = AuthRepository(sessionManager)
        reservationRepository = ReservationRepository()
        tourRepository = TourRepository()
        settingsRepository = SettingsRepository()
        userRepository = UserRepository()
        gcalRepository = GoogleCalendarRepository()
        notificationRepository = NotificationRepository(sessionManager)
        benefitRepository = BenefitRepository()
        blogRepository = BlogRepository()
        planConfigRepository = PlanConfigRepository()
        activityRepository = ActivityRepository()
        uploadRepository = UploadRepository(applicationContext)

        // Si el usuario ya está logueado, registrar el token FCM actual
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (sessionManager.getTokenBlocking() != null) {
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        Log.d("TEMApp", "Token FCM actual: $token")
                        CoroutineScope(Dispatchers.IO).launch {
                            notificationRepository.registerToken(token)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TEMApp", "Error inicializando FCM", e)
            }
        }
    }
}
