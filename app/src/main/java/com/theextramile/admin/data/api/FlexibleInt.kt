package com.theextramile.admin.data.api

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Lee un entero venga como número o como texto, y lo escribe SIEMPRE como
 * número entero.
 *
 * Hace falta porque los importes de una reserva no tienen un tipo fijo en el
 * servidor: unas veces `"total": 840000` y otras `"total": "840.000"` (según
 * si la reserva entró por la web o se creó a mano).
 *
 * ⚠️ Lo importante es la ESCRITURA. Si estos campos se declararan `Any?`,
 * Gson los leería como Double y al guardar escribiría `840000.0`; el backend
 * se queda con los dígitos al parsear y eso convertiría 840.000 en 8.400.000.
 * Por eso se escribe entero pelado.
 */
class FlexibleIntAdapter : TypeAdapter<Int?>() {

    override fun write(out: JsonWriter, value: Int?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): Int? = when (reader.peek()) {
        JsonToken.NULL -> {
            reader.nextNull()
            null
        }
        JsonToken.NUMBER -> reader.nextDouble().let { Math.round(it).toInt() }
        JsonToken.STRING -> {
            // "840.000" y "$840.000" → 840000, igual que payParse() del panel
            val digits = reader.nextString().filter { it.isDigit() }
            if (digits.isEmpty()) null else digits.toIntOrNull()
        }
        JsonToken.BOOLEAN -> {
            reader.nextBoolean()
            null
        }
        else -> {
            reader.skipValue()
            null
        }
    }
}
