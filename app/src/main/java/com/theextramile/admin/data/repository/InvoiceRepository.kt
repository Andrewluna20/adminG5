package com.theextramile.admin.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.theextramile.admin.BuildConfig
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.data.model.SiteSettings
import com.theextramile.admin.util.PhoneUtil

/**
 * Genera la URL de la factura PDF en el servidor y la envía por WhatsApp.
 *
 * El PDF se genera en el servidor:
 *   {api_base}/invoice.php?id={reservationId}
 *
 * La app NO genera el PDF — solo envía un mensaje WhatsApp con el link.
 */
class InvoiceRepository {

    companion object {
        private const val TAG = "InvoiceRepo"
        private val DEFAULT_INVOICE_MESSAGE = """
¡Hola {name}! 🎉

Aquí está tu factura para *{tour}*.

📋 Código: #{id}
👥 Pasajeros: {pax}
📅 Fecha: {date}

📄 Descarga tu factura:
{invoice}

¡Gracias por elegirnos! 🌊
""".trimIndent()
    }

    /**
     * Construye la URL de la factura (PDF que el cliente puede abrir).
     *
     * ⚠️ `action=realpdf` es obligatorio. Sin él, invoice.php cae en su
     * acción por defecto ('preview'), que devuelve la factura como página
     * HTML pensada para imprimir con Ctrl+P desde un computador. Al
     * cliente, que abre el enlace en el teléfono, eso le llega como una
     * página web y no como el PDF que espera. Con `realpdf` devuelve el
     * PDF de verdad, el mismo que se adjunta al correo de confirmación.
     */
    fun getInvoiceUrl(reservationId: String): String {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        return "$base/invoice.php?action=realpdf&id=$reservationId"
    }

    /**
     * Genera el mensaje de WhatsApp con placeholders reemplazados.
     */
    fun buildInvoiceMessage(
        reservation: Reservation,
        settings: SiteSettings,
        invoiceUrl: String
    ): String {
        val template = settings.invoiceMessage.ifEmpty { DEFAULT_INVOICE_MESSAGE }

        return template
            .replace("{name}", reservation.name)
            .replace("{tour}", reservation.tourTitle)
            .replace("{id}", reservation.id)
            .replace("{pax}", reservation.pax.toString())
            .replace("{date}", reservation.date)
            .replace("{phone}", reservation.phone)
            .replace("{email}", reservation.email ?: "")
            .replace("{invoice}", invoiceUrl)
    }

    /**
     * Abre WhatsApp con el mensaje pre-cargado al número del cliente.
     */
    fun sendInvoiceViaWhatsApp(
        context: Context,
        reservation: Reservation,
        settings: SiteSettings
    ): Boolean {
        val invoiceUrl = getInvoiceUrl(reservation.id)
        val message = buildInvoiceMessage(reservation, settings, invoiceUrl)

        return try {
            PhoneUtil.openWhatsApp(context, reservation.phone, message)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo WhatsApp", e)
            false
        }
    }

    /**
     * Abre el PDF de la factura en el navegador para previsualizarla.
     */
    fun previewInvoice(context: Context, reservationId: String): Boolean {
        val url = getInvoiceUrl(reservationId)
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo factura", e)
            false
        }
    }
}
