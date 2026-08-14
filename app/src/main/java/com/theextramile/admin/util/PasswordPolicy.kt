package com.theextramile.admin.util

/**
 * La política de contraseñas del panel, letra por letra.
 *
 * Espejo de passwordPolicyError() en admin-js/users.js:26. Tiene que decir
 * lo MISMO que el panel: si la app aceptara una contraseña que el servidor
 * rechaza, el usuario vería un error genérico sin saber qué le falta.
 *
 * ⚠️ Las comprobaciones van con rangos ASCII a propósito, no con
 * `isUpperCase()` ni `isLetterOrDigit()`. El panel usa [A-Z], [a-z] y
 * [^a-zA-Z0-9], que son ASCII; los de Kotlin entienden Unicode, y entonces
 * una "Ñ" contaría como mayúscula aquí pero no allá, y una "ñ" contaría
 * como símbolo allá pero no aquí. La misma contraseña daría veredictos
 * distintos según por dónde se cambie.
 *
 * Devuelve null si la contraseña vale, o el motivo si no.
 */
fun passwordPolicyError(pass: String): String? = when {
    pass.length < 8 ->
        "La contraseña debe tener al menos 8 caracteres."
    pass.none { it in 'A'..'Z' } ->
        "La contraseña debe incluir al menos una letra MAYÚSCULA."
    pass.none { it in 'a'..'z' } ->
        "La contraseña debe incluir al menos una letra minúscula."
    pass.none { !esAlfanumericoAscii(it) } ->
        "La contraseña debe incluir al menos un símbolo (ej: ! @ # $ % . -)."
    else -> null
}

private fun esAlfanumericoAscii(c: Char): Boolean =
    c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9'
