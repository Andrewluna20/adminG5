package com.theextramile.admin.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.theextramile.admin.data.model.SiteSettings
import com.theextramile.admin.ui.blog.absoluteUrl
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*
import com.theextramile.admin.util.payParse

/**
 * Ajustes — la lista de sub-secciones y el editor de cada una.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    siteBaseUrl: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val section by viewModel.section.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    // Dentro de una sub-sección, "atrás" vuelve a la lista, no sale de Ajustes
    BackHandler(enabled = section != null) { viewModel.openSection(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Gradients.Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            SectionHeader(
                title = section?.title ?: "Ajustes",
                subtitle = section?.description ?: "Configuración del sitio público",
                onBack = { if (section != null) viewModel.openSection(null) else onBack() }
            )

            when {
                uiState.isLoading -> SectionPlaceholder("Cargando…", isLoading = true)

                section == null -> SubSectionList(
                    modifier = Modifier.weight(1f),
                    onOpen = viewModel::openSection
                )

                else -> Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    SubSectionContent(section!!, viewModel, draft, uiState, siteBaseUrl)
                    Spacer(Modifier.height(32.dp))
                }
            }

            SaveBar(
                visible = viewModel.hasChanges && !uiState.isLoading,
                isSaving = uiState.isSaving,
                onSave = { viewModel.save() }
            )
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp))
    }
}

@Composable
private fun SubSectionList(
    modifier: Modifier = Modifier,
    onOpen: (SettingsViewModel.SubSection) -> Unit
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SettingsViewModel.SubSection.entries.forEach { s ->
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                onClick = { onOpen(s) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftIconBox(iconFor(s), size = 38.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(s.description, color = TextDim, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                        tint = TextMuted, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun iconFor(s: SettingsViewModel.SubSection) = when (s) {
    SettingsViewModel.SubSection.BRAND -> Icons.Default.Storefront
    SettingsViewModel.SubSection.HERO -> Icons.Default.Wallpaper
    SettingsViewModel.SubSection.TOURS_TEXT -> Icons.Default.TextFields
    SettingsViewModel.SubSection.COLORS -> Icons.Default.Palette
    SettingsViewModel.SubSection.COMPANY -> Icons.Default.Business
    SettingsViewModel.SubSection.WHATSAPP -> Icons.Default.Chat
    SettingsViewModel.SubSection.OPERADOR -> Icons.Default.SupportAgent
    SettingsViewModel.SubSection.GCAL -> Icons.Default.CalendarMonth
    SettingsViewModel.SubSection.BOLD -> Icons.Default.CreditCard
    SettingsViewModel.SubSection.RECAPTCHA -> Icons.Default.Security
    SettingsViewModel.SubSection.FAVICON -> Icons.Default.Bookmark
    SettingsViewModel.SubSection.POLICY -> Icons.Default.Gavel
    SettingsViewModel.SubSection.INTEGRATIONS -> Icons.Default.Link
    SettingsViewModel.SubSection.BANKS -> Icons.Default.ViewList
}

@Composable
private fun SubSectionContent(
    section: SettingsViewModel.SubSection,
    vm: SettingsViewModel,
    d: SiteSettings,
    uiState: SettingsViewModel.UiState,
    siteBaseUrl: String
) = when (section) {
    SettingsViewModel.SubSection.BRAND -> BrandSection(vm, d, uiState, siteBaseUrl)
    SettingsViewModel.SubSection.HERO -> HeroSection(vm, d, uiState, siteBaseUrl)
    SettingsViewModel.SubSection.TOURS_TEXT -> ToursTextSection(vm, d)
    SettingsViewModel.SubSection.COLORS -> ColorsSection(vm, d)
    SettingsViewModel.SubSection.COMPANY -> CompanySection(vm, d, uiState, siteBaseUrl)
    SettingsViewModel.SubSection.WHATSAPP -> WhatsAppSection(vm, d)
    SettingsViewModel.SubSection.OPERADOR -> OperadorSection(vm, d)
    SettingsViewModel.SubSection.GCAL -> GcalSection(vm, d)
    SettingsViewModel.SubSection.BOLD -> BoldSection(vm, d)
    SettingsViewModel.SubSection.RECAPTCHA -> RecaptchaSection(vm)
    SettingsViewModel.SubSection.FAVICON -> FaviconSection(vm, d, uiState, siteBaseUrl)
    SettingsViewModel.SubSection.POLICY -> PolicySection(vm, d)
    SettingsViewModel.SubSection.INTEGRATIONS -> IntegrationsSection(vm, d)
    SettingsViewModel.SubSection.BANKS -> BanksSection(vm, d)
}

// ═══════════════════════════════════════════
// Sub-secciones
// ═══════════════════════════════════════════

@Composable
private fun BrandSection(
    vm: SettingsViewModel,
    d: SiteSettings,
    uiState: SettingsViewModel.UiState,
    siteBaseUrl: String
) {
    AdminField("Nombre del sitio", d.siteName, { v -> vm.update { it.copy(siteName = v) } })
    ColorField("Color de marca", d.siteBrandColor) { v -> vm.update { it.copy(siteBrandColor = v) } }
    ColorField("Color de acento", d.siteAccentColor) { v -> vm.update { it.copy(siteAccentColor = v) } }
    AdminField(
        "Fuente", d.siteBrandFont, { v -> vm.update { it.copy(siteBrandFont = v) } },
        hint = "El nombre exacto de una fuente de Google Fonts, p. ej. DM Sans"
    )

    FormSectionTitle("Logo de la barra")
    ImageUploadRow(vm, uiState, siteBaseUrl, d.siteLogo, SettingsViewModel.ImageTarget.SITE_LOGO)

    FormSectionTitle("Logo sobre la portada")
    Text(
        "El segundo logo, el que se ve encima del hero.",
        color = TextDim, fontSize = 11.sp, lineHeight = 15.sp
    )
    ImageUploadRow(vm, uiState, siteBaseUrl, d.siteLogoHero, SettingsViewModel.ImageTarget.SITE_LOGO_HERO)
}

@Composable
private fun HeroSection(
    vm: SettingsViewModel,
    d: SiteSettings,
    uiState: SettingsViewModel.UiState,
    siteBaseUrl: String
) {
    AdminField(
        "Título", d.heroTitle, { v -> vm.update { it.copy(heroTitle = v) } },
        hint = "Acepta HTML: <br> para saltos y <em> para la parte en cursiva",
        singleLine = false, minLines = 2
    )
    AdminField("Subtítulo", d.heroSubtitle, { v -> vm.update { it.copy(heroSubtitle = v) } })
    AdminField(
        "Texto pequeño (celular)", d.mobileHeroEyebrow,
        { v -> vm.update { it.copy(mobileHeroEyebrow = v) } }
    )

    FormSectionTitle("Fondo")
    FilterChipRow(
        options = listOf(
            "gradient" to "Degradado",
            "color" to "Color liso",
            "image" to "Imagen",
            "video" to "Vídeo"
        ),
        selected = d.heroBgType,
        onSelect = { v -> vm.update { it.copy(heroBgType = v) } },
        horizontalPadding = 0.dp
    )

    when (d.heroBgType) {
        "color" -> ColorField("Color del fondo", d.heroBgColor) { v ->
            vm.update { it.copy(heroBgColor = v) }
        }

        "gradient" -> {
            ColorField("Degradado — inicio", d.heroGradientStart) { v -> vm.update { it.copy(heroGradientStart = v) } }
            ColorField("Degradado — medio", d.heroGradientMid) { v -> vm.update { it.copy(heroGradientMid = v) } }
            ColorField("Degradado — fin", d.heroGradientEnd) { v -> vm.update { it.copy(heroGradientEnd = v) } }
        }

        "image" -> {
            Spacer(Modifier.height(10.dp))
            ImageUploadRow(vm, uiState, siteBaseUrl, d.heroBgImage, SettingsViewModel.ImageTarget.HERO_IMAGE)
        }

        "video" -> {
            FormSectionTitle("Vídeo de la portada")
            Text(
                if (d.heroBgVideo.isBlank()) "Todavía no hay vídeo"
                else "Vídeo actual: ${d.heroBgVideo.substringAfterLast('/')}",
                color = TextSecondary, fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            UploadButton(vm, uiState, SettingsViewModel.ImageTarget.HERO_VIDEO, "Subir vídeo MP4")
            FormSectionTitle("Imagen de espera (póster)")
            Text(
                "Se ve mientras el vídeo carga.",
                color = TextDim, fontSize = 11.sp
            )
            ImageUploadRow(vm, uiState, siteBaseUrl, d.heroPosterImage, SettingsViewModel.ImageTarget.HERO_POSTER)
        }
    }
}

@Composable
private fun ToursTextSection(vm: SettingsViewModel, d: SiteSettings) {
    AdminField(
        "Etiqueta", d.toursLabel, { v -> vm.update { it.copy(toursLabel = v) } },
        hint = "El texto pequeño encima del título, p. ej. «Nuestros Destinos»"
    )
    AdminField("Título", d.toursTitle, { v -> vm.update { it.copy(toursTitle = v) } })

    FormSectionTitle("Versión en inglés (/en)")
    AdminField("Etiqueta (EN)", d.toursLabelEn, { v -> vm.update { it.copy(toursLabelEn = v) } })
    AdminField("Título (EN)", d.toursTitleEn, { v -> vm.update { it.copy(toursTitleEn = v) } })
}

@Composable
private fun ColorsSection(vm: SettingsViewModel, d: SiteSettings) {
    Text(
        "Son los colores del SITIO PÚBLICO, no los de esta app.",
        color = TextDim, fontSize = 11.sp, lineHeight = 15.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
    FormSectionTitle("Portada")
    ColorField("Degradado — inicio", d.heroGradientStart) { v -> vm.update { it.copy(heroGradientStart = v) } }
    ColorField("Degradado — medio", d.heroGradientMid) { v -> vm.update { it.copy(heroGradientMid = v) } }
    ColorField("Degradado — fin", d.heroGradientEnd) { v -> vm.update { it.copy(heroGradientEnd = v) } }

    FormSectionTitle("Pie de página")
    ColorField("Fondo del pie", d.footerBg) { v -> vm.update { it.copy(footerBg = v) } }
    ColorField("Texto del pie", d.footerText) { v -> vm.update { it.copy(footerText = v) } }

    FormSectionTitle("Botones")
    ColorField("Fondo del botón", d.buttonBg) { v -> vm.update { it.copy(buttonBg = v) } }
    ColorField("Texto del botón", d.buttonText) { v -> vm.update { it.copy(buttonText = v) } }
    ColorField("Fondo de «Reservar»", d.reserveBtnBg) { v -> vm.update { it.copy(reserveBtnBg = v) } }
    ColorField("Texto de «Reservar»", d.reserveBtnText) { v -> vm.update { it.copy(reserveBtnText = v) } }
}

@Composable
private fun CompanySection(
    vm: SettingsViewModel,
    d: SiteSettings,
    uiState: SettingsViewModel.UiState,
    siteBaseUrl: String
) {
    Text(
        "Estos datos salen en la factura que recibe el cliente.",
        color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
    )
    AdminField("Razón social", d.companyName, { v -> vm.update { it.copy(companyName = v) } })
    AdminField("Eslogan", d.companyTagline, { v -> vm.update { it.copy(companyTagline = v) } })
    AdminField("NIT", d.companyNit, { v -> vm.update { it.copy(companyNit = v) } })
    AdminField("Dirección", d.companyAddress, { v -> vm.update { it.copy(companyAddress = v) } })
    AdminField("Teléfono", d.companyPhone, { v -> vm.update { it.copy(companyPhone = v) } })
    AdminField("Correo", d.companyEmail, { v -> vm.update { it.copy(companyEmail = v) } })

    FormSectionTitle("Logo de la factura")
    ImageUploadRow(vm, uiState, siteBaseUrl, d.companyLogo, SettingsViewModel.ImageTarget.COMPANY_LOGO)

    FormSectionTitle("Correo de confirmación")
    AdminSwitch(
        "Enviar automáticamente", d.confirmEmailEnabled,
        { v -> vm.update { it.copy(confirmEmailEnabled = v) } },
        hint = "El correo con el tiquete que se manda al confirmar una reserva"
    )
    TestEmailRow(vm, uiState, d.companyEmail)
}

@Composable
private fun WhatsAppSection(vm: SettingsViewModel, d: SiteSettings) {
    AdminField(
        "WhatsApp del negocio", d.adminWhatsApp,
        { v -> vm.update { it.copy(adminWhatsApp = v) } },
        placeholder = "+573001234567",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
    )

    FormSectionTitle("Mensaje al cliente")
    AdminSwitch(
        "Usar mi mensaje personalizado", d.whatsappCustomEnabled,
        { v -> vm.update { it.copy(whatsappCustomEnabled = v) } },
        hint = "Si está apagado se usa el mensaje que trae el sistema"
    )
    AdminField(
        "Plantilla", d.whatsappDefaultMessage,
        { v -> vm.update { it.copy(whatsappDefaultMessage = v) } },
        hint = PLACEHOLDER_HINT,
        singleLine = false, minLines = 5
    )

    FormSectionTitle("Mensaje de la factura")
    AdminField(
        "Plantilla", d.invoiceMessage, { v -> vm.update { it.copy(invoiceMessage = v) } },
        hint = "El que acompaña a la factura cuando se la mandas al cliente",
        singleLine = false, minLines = 3
    )

    FormSectionTitle("Mensaje antiguo")
    AdminField(
        "waMessage", d.waMessage, { v -> vm.update { it.copy(waMessage = v) } },
        hint = "Campo heredado del panel viejo. Déjalo como está si no sabes qué es.",
        singleLine = false, minLines = 2
    )
}

@Composable
private fun OperadorSection(vm: SettingsViewModel, d: SiteSettings) {
    Text(
        "El aviso que le llega al operador cuando entra una reserva.",
        color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
    )
    AdminSwitch(
        "Usar mi mensaje personalizado", d.operatorCustomEnabled,
        { v -> vm.update { it.copy(operatorCustomEnabled = v) } }
    )
    AdminField(
        "Plantilla", d.operatorDefaultMessage,
        { v -> vm.update { it.copy(operatorDefaultMessage = v) } },
        hint = PLACEHOLDER_HINT,
        singleLine = false, minLines = 6
    )
}

@Composable
private fun GcalSection(vm: SettingsViewModel, d: SiteSettings) {
    FilterChipRow(
        options = listOf("webhook" to "Webhook", "oauth" to "Cuenta de Google"),
        selected = d.gcalMethod,
        onSelect = { v -> vm.update { it.copy(gcalMethod = v) } },
        horizontalPadding = 0.dp
    )
    Spacer(Modifier.height(6.dp))
    Text(
        if (d.gcalMethod == "webhook")
            "Con webhook los eventos se mandan a una URL tuya (por ejemplo un Apps Script)."
        else "Con cuenta de Google los eventos van directo al calendario. " +
            "Las cuentas se vinculan desde la sección Google Calendar.",
        color = TextDim, fontSize = 11.sp, lineHeight = 15.sp
    )

    if (d.gcalMethod == "webhook") {
        AdminField(
            "URL del webhook", d.gcalWebhookUrl,
            { v -> vm.update { it.copy(gcalWebhookUrl = v) } }
        )
    } else {
        Spacer(Modifier.height(10.dp))
        Text(
            if (d.gcalConnected) "✓ Hay una cuenta vinculada" else "Sin cuenta vinculada todavía",
            color = if (d.gcalConnected) GreenLight else Yellow, fontSize = 12.sp
        )
    }

    FormSectionTitle("Plantillas del evento")
    AdminField(
        "Título del evento", d.gcalTitleTemplate,
        { v -> vm.update { it.copy(gcalTitleTemplate = v) } },
        hint = PLACEHOLDER_HINT
    )
    AdminField(
        "Descripción del evento", d.gcalDescTemplate,
        { v -> vm.update { it.copy(gcalDescTemplate = v) } },
        singleLine = false, minLines = 6
    )
}

@Composable
private fun BoldSection(vm: SettingsViewModel, d: SiteSettings) {
    val secret by vm.boldSecret.collectAsState()

    AdminSwitch("Cobros con Bold activados", d.boldEnabled, { v -> vm.update { it.copy(boldEnabled = v) } })
    AdminSwitch(
        "Modo de pruebas", d.boldTestMode, { v -> vm.update { it.copy(boldTestMode = v) } },
        hint = "En pruebas no se cobra de verdad"
    )
    AdminField("Llave pública (API key)", d.boldApiKey, { v -> vm.update { it.copy(boldApiKey = v) } })

    SecretField(
        label = "Llave secreta",
        value = secret,
        onChange = vm::onBoldSecretChange
    )

    FormSectionTitle("Cuánto se cobra")
    FilterChipRow(
        options = listOf("full" to "El total", "deposit" to "Un abono"),
        selected = d.boldChargeMode,
        onSelect = { v -> vm.update { it.copy(boldChargeMode = v) } },
        horizontalPadding = 0.dp
    )
    if (d.boldChargesDeposit) {
        AdminField(
            "Porcentaje del abono", d.boldDepositPercent.toString(),
            { v -> vm.update { it.copy(boldDepositPercent = payParse(v).coerceIn(1, 100)) } },
            hint = "Entre 1 y 100",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun RecaptchaSection(vm: SettingsViewModel) {
    val draft by vm.draft.collectAsState()
    val secret by vm.recaptchaSecret.collectAsState()

    Text(
        "Protege el formulario público contra robots.",
        color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
    )
    AdminField(
        "Clave del sitio", draft.recaptchaSiteKey,
        { v -> vm.update { it.copy(recaptchaSiteKey = v) } }
    )
    SecretField(
        label = "Clave secreta",
        value = secret,
        onChange = vm::onRecaptchaSecretChange
    )
}

@Composable
private fun FaviconSection(
    vm: SettingsViewModel,
    d: SiteSettings,
    uiState: SettingsViewModel.UiState,
    siteBaseUrl: String
) {
    Text(
        "El iconito de la pestaña del navegador. Lo ideal es un PNG cuadrado de 512×512.",
        color = TextDim, fontSize = 11.sp, lineHeight = 15.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
    Spacer(Modifier.height(10.dp))
    ImageUploadRow(vm, uiState, siteBaseUrl, d.faviconUrl, SettingsViewModel.ImageTarget.FAVICON, imageHeight = 90.dp)
}

@Composable
private fun PolicySection(vm: SettingsViewModel, d: SiteSettings) {
    AdminField(
        "Resumen", d.ticketPolicyShort, { v -> vm.update { it.copy(ticketPolicyShort = v) } },
        hint = "El párrafo corto que va dentro del tiquete",
        singleLine = false, minLines = 4
    )
    AdminField(
        "Texto completo", d.ticketPolicyFull, { v -> vm.update { it.copy(ticketPolicyFull = v) } },
        hint = "La política entera, la que se enlaza desde el sitio",
        singleLine = false, minLines = 8
    )
}

@Composable
private fun IntegrationsSection(vm: SettingsViewModel, d: SiteSettings) {
    Text(
        "Llaves de servicios externos que usa el sitio público.",
        color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
    )
    AdminField(
        "Llave de Google Maps", d.mapsApiKey, { v -> vm.update { it.copy(mapsApiKey = v) } },
        hint = "La usan los mapas incrustados de los planes y de los beneficios"
    )

    FormSectionTitle("Webhook antiguo del calendario")
    AdminField(
        "calendarWebhook", d.calendarWebhook,
        { v -> vm.update { it.copy(calendarWebhook = v) } },
        hint = "Campo heredado del panel viejo. El webhook que se usa hoy está en " +
            "Google Calendar; este solo sigue aquí por las instalaciones antiguas."
    )
}

@Composable
private fun BanksSection(vm: SettingsViewModel, d: SiteSettings) {
    Text(
        "Bloques que se escriben una vez y luego cada plan marca los que usa.",
        color = TextDim, fontSize = 11.sp, lineHeight = 15.sp,
        modifier = Modifier.padding(top = 8.dp)
    )

    FormSectionTitle("Preguntas frecuentes (${d.faqs.size})")
    d.faqs.forEach { faq ->
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column {
                AdminField("Pregunta", faq.q, { v -> vm.updateFaq(faq.id, v, faq.a) })
                AdminField("Respuesta", faq.a, { v -> vm.updateFaq(faq.id, faq.q, v) }, singleLine = false, minLines = 3)
                TextButton(onClick = { vm.removeFaq(faq.id) }) {
                    Text("Eliminar", color = OrangeRed, fontSize = 12.sp)
                }
            }
        }
    }
    OutlinedButton(
        onClick = { vm.addFaq() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanLight)
    ) { Text("Añadir pregunta") }

    FormSectionTitle("Bloques de información (${d.infos.size})")
    d.infos.forEach { info ->
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column {
                AdminField("Título", info.title, { v -> vm.updateInfo(info.id, v, info.text) })
                AdminField("Texto", info.text, { v -> vm.updateInfo(info.id, info.title, v) }, singleLine = false, minLines = 3)
                TextButton(onClick = { vm.removeInfo(info.id) }) {
                    Text("Eliminar", color = OrangeRed, fontSize = 12.sp)
                }
            }
        }
    }
    OutlinedButton(
        onClick = { vm.addInfo() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanLight)
    ) { Text("Añadir bloque") }

    FormSectionTitle("Horarios informativos (${d.schedules.size})")
    Text(
        "Son solo texto: no son los horarios que el cliente elige al reservar.",
        color = TextDim, fontSize = 11.sp
    )
    d.schedules.forEach { sch ->
        GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
            Column {
                AdminField("Texto", sch.text, { v -> vm.updateSchedule(sch.id, v) }, singleLine = false, minLines = 2)
                TextButton(onClick = { vm.removeSchedule(sch.id) }) {
                    Text("Eliminar", color = OrangeRed, fontSize = 12.sp)
                }
            }
        }
    }
    OutlinedButton(
        onClick = { vm.addSchedule() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanLight)
    ) { Text("Añadir horario") }

    FormSectionTitle("Etiquetas de planes (${d.planTags.size})")
    d.planTags.forEach { tag ->
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column {
                AdminField("Nombre", tag.name, { v -> vm.updateTag(tag.id, v, tag.color) })
                ColorField("Color", tag.color) { v -> vm.updateTag(tag.id, tag.name, v) }
                TextButton(onClick = { vm.removeTag(tag.id) }) {
                    Text("Eliminar", color = OrangeRed, fontSize = 12.sp)
                }
            }
        }
    }
    OutlinedButton(
        onClick = { vm.addTag() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanLight)
    ) { Text("Añadir etiqueta") }
}

// ═══════════════════════════════════════════
// Piezas reutilizadas dentro de Ajustes
// ═══════════════════════════════════════════

private const val PLACEHOLDER_HINT =
    "Marcadores disponibles: {name} {tour} {date} {pax} {phone} {email} {id} {notes} {seller}"

@Composable
private fun ImageUploadRow(
    vm: SettingsViewModel,
    uiState: SettingsViewModel.UiState,
    siteBaseUrl: String,
    currentUrl: String,
    target: SettingsViewModel.ImageTarget,
    imageHeight: androidx.compose.ui.unit.Dp = 130.dp
) {
    if (currentUrl.isNotBlank()) {
        AsyncImage(
            model = absoluteUrl(currentUrl, siteBaseUrl),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.height(8.dp))
    }
    UploadButton(
        vm, uiState, target,
        if (currentUrl.isBlank()) "Subir imagen" else "Cambiar imagen"
    )
}

@Composable
private fun UploadButton(
    vm: SettingsViewModel,
    uiState: SettingsViewModel.UiState,
    target: SettingsViewModel.ImageTarget,
    label: String
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) vm.uploadImage(uri, target) }

    GradientButton(
        text = label,
        onClick = { picker.launch(target.mimeFilter) },
        isLoading = uiState.isUploading,
        icon = if (target == SettingsViewModel.ImageTarget.HERO_VIDEO) Icons.Default.Movie else Icons.Default.Image,
        height = 46.dp,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Campo de secreto. El servidor nunca devuelve estos valores, así que se
 * muestra siempre vacío: escribir algo lo reemplaza, dejarlo vacío no lo toca.
 */
@Composable
private fun SecretField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label.uppercase(), color = TextMuted, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Sin cambios", color = TextDim, fontSize = 13.sp) },
            singleLine = true,
            visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None
            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        "Ver", tint = TextMuted
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = adminFieldColors()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Por seguridad el servidor no devuelve esta llave. Déjalo vacío para no cambiarla.",
            color = TextDim, fontSize = 11.sp, lineHeight = 15.sp
        )
    }
}

@Composable
private fun TestEmailRow(
    vm: SettingsViewModel,
    uiState: SettingsViewModel.UiState,
    defaultEmail: String
) {
    var to by remember(defaultEmail) { mutableStateOf(defaultEmail) }
    AdminField(
        "Correo de prueba", to, { to = it },
        hint = "Manda un correo de confirmación de ejemplo para ver cómo queda",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
    GradientButton(
        text = "Enviar correo de prueba",
        onClick = { vm.sendTestEmail(to) },
        isLoading = uiState.isSendingTest,
        gradient = Gradients.GreenCyan,
        icon = Icons.Default.Send,
        height = 44.dp,
        modifier = Modifier.fillMaxWidth()
    )
}
