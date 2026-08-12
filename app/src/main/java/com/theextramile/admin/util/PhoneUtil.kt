package com.theextramile.admin.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.theextramile.admin.data.model.Reservation

object PhoneUtil {

    /** Limpia un teléfono: deja solo dígitos y el "+" inicial */
    fun cleanPhone(phone: String): String {
        return phone.trim()
            .replace(Regex("[^+0-9]"), "")
            .let { if (it.startsWith("+")) "+" + it.drop(1).filter { c -> c.isDigit() } else it.filter { c -> c.isDigit() } }
    }

    /** Reemplaza placeholders del mensaje */
    fun replacePlaceholders(template: String, r: Reservation): String {
        return template
            .replace("{name}", r.name)
            .replace("{tour}", r.tourTitle)
            .replace("{date}", r.date)
            .replace("{pax}", r.pax.toString())
            .replace("{phone}", r.phone)
            .replace("{email}", r.email ?: "")
            .replace("{notes}", r.notes ?: "")
            .replace("{id}", r.id)
    }

    /** Mensaje por defecto si no hay plantilla configurada */
    fun defaultWaMessage(r: Reservation): String {
        return """
        ¡Hola ${r.name}! 👋
        
        Te escribimos de *The Extra Mile* sobre tu reserva.
        
        🌴 *Plan:* ${r.tourTitle}
        📅 *Fecha:* ${r.date}
        👥 *Pasajeros:* ${r.pax}
        🔖 *Código:* #${r.id}
        
        ¡Estamos a tu disposición!
        """.trimIndent()
    }

    /** Mensaje para reenviar la reserva a tu propio número (resumen interno) */
    fun forwardWaMessage(r: Reservation): String {
        return """
        📋 *NUEVA RESERVA RECIBIDA*
        
        👤 *Cliente:* ${r.name}
        📞 *Teléfono:* ${r.phone}
        ${if (!r.email.isNullOrBlank()) "📧 *Email:* ${r.email}\n" else ""}🌴 *Plan:* ${r.tourTitle}
        📅 *Fecha:* ${r.date}
        👥 *Pasajeros:* ${r.pax}
        ${if (!r.notes.isNullOrBlank()) "📝 *Notas:* ${r.notes}\n" else ""}🔖 *Código:* #${r.id}
        🏷 *Estado:* ${r.statusDisplay}
        """.trimIndent()
    }

    /** Abre WhatsApp con el mensaje */
    fun openWhatsApp(context: Context, phone: String, message: String) {
        val clean = cleanPhone(phone)
        if (clean.isBlank()) {
            Toast.makeText(context, "Teléfono inválido", Toast.LENGTH_SHORT).show()
            return
        }
        val encoded = Uri.encode(message)
        val url = "https://wa.me/$clean?text=$encoded"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    /** Abre el marcador del teléfono */
    fun openDialer(context: Context, phone: String) {
        val clean = cleanPhone(phone)
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
        }
    }
}
