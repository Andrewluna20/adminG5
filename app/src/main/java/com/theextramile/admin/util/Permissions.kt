package com.theextramile.admin.util

/**
 * Roles y permisos — espejo exacto del bloque "PERMISOS POR ROL" de
 * admin-js/core.js. Si cambias algo aquí, cámbialo también allá.
 *
 * Los roles reales son 'super', 'reservations' y 'viewer'. Ojo: 'viewer' se
 * MUESTRA como "Editor" (antes decía "Solo Ver"), y cualquier rol legado o
 * desconocido (p. ej. el antiguo 'admin' que dejó un bug del backend) se trata
 * como el más limitado, 'reservations'.
 *
 * Estos permisos son de interfaz: el backend los vuelve a comprobar en cada
 * endpoint con requirePanelAdmin(). Ocultar un botón aquí no es la seguridad,
 * es la comodidad.
 */
object Roles {
    const val SUPER = "super"
    const val RESERVATIONS = "reservations"
    const val VIEWER = "viewer"

    /** normRole() */
    fun norm(role: String?): String = when (role) {
        SUPER, RESERVATIONS, VIEWER -> role
        else -> RESERVATIONS
    }

    /** roleLabel() */
    fun label(role: String?): String = when (norm(role)) {
        SUPER -> "Super Admin"
        VIEWER -> "Editor"
        else -> "Gestor de Reservas"
    }

    /** Secciones que ve todo el mundo */
    private val NAV_BASE = listOf(Section.OVERVIEW, Section.RESERVATIONS, Section.CALENDAR)

    /** Lo que suma el Editor ('viewer') sobre la base */
    private val NAV_EDITOR_EXTRA = listOf(Section.TOURS, Section.EXTRACTO, Section.BENEFITS)

    /** navAllowedFor() */
    fun canSee(role: String?, section: Section): Boolean = when (norm(role)) {
        SUPER -> true
        VIEWER -> section in NAV_BASE || section in NAV_EDITOR_EXTRA
        else -> section in NAV_BASE
    }

    /** Secciones visibles para un rol, en el orden del menú lateral */
    fun sectionsFor(role: String?): List<Section> = Section.entries.filter { canSee(role, it) }

    /**
     * canManageReservations(): confirmar, registrar pago, cambiar fecha,
     * cancelar y crear reservas a mano. Los tres roles pueden.
     */
    fun canManageReservations(role: String?): Boolean =
        norm(role) in listOf(SUPER, RESERVATIONS, VIEWER)

    /** canEditNet(): ver y editar el precio neto (costo interno). Solo Super. */
    fun canEditNet(role: String?): Boolean = norm(role) == SUPER

    /** Crear, editar y borrar planes. Solo Super. */
    fun canEditTours(role: String?): Boolean = norm(role) == SUPER

    /** Gestionar usuarios del panel. Solo Super. */
    fun canManageUsers(role: String?): Boolean = norm(role) == SUPER
}

/**
 * Las 13 secciones del panel, en el mismo orden que admin-html/sidebar.html.
 */
enum class Section(
    val key: String,
    val title: String,
    val group: SectionGroup
) {
    OVERVIEW("overview", "Resumen", SectionGroup.GENERAL),
    RESERVATIONS("reservations", "Reservas", SectionGroup.GENERAL),
    CALENDAR("calendar", "Calendario", SectionGroup.GENERAL),
    TOURS("tours", "Planes", SectionGroup.SITE),
    PLAN_CONFIG("plan-config", "Config. de planes", SectionGroup.SITE),
    BENEFITS("benefits", "Beneficios", SectionGroup.SITE),
    BLOG("blog", "Blog", SectionGroup.SITE),
    SEO("seo", "SEO", SectionGroup.SITE),
    EXTRACTO("extracto", "Extracto", SectionGroup.SITE),
    ACTIVITY("activity", "Actividad", SectionGroup.SITE),
    SETTINGS("settings", "Ajustes", SectionGroup.SITE),
    USERS("users", "Usuarios", SectionGroup.USERS),
    GCAL("gcal", "Google Calendar", SectionGroup.USERS);

    companion object {
        fun fromKey(key: String?): Section = entries.firstOrNull { it.key == key } ?: OVERVIEW
    }
}

/** Los tres encabezados del menú lateral */
enum class SectionGroup(val title: String) {
    GENERAL("General"),
    SITE("Gestión del Sitio"),
    USERS("Usuarios")
}
