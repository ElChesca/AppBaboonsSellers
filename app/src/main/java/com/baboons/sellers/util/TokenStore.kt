package com.baboons.sellers.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Almacenamiento seguro (AES-256) para el JWT token y datos de sesión.
 */
object TokenStore {

    private const val PREFS_NAME = "baboons_sellers_secure"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_NOMBRE = "user_nombre"
    private const val KEY_ROL = "user_rol"
    private const val KEY_NEGOCIO_ID = "negocio_id"
    private const val KEY_NEGOCIO_NOMBRE = "negocio_nombre"
    private const val KEY_HOJA_RUTA_ID = "hoja_ruta_id"

    private fun getPrefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSessionImmediate(
        context: Context,
        token: String,
        nombre: String,
        rol: String,
        negocioId: Int?,
        negocioNombre: String?
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_NOMBRE, nombre)
            putString(KEY_ROL, rol)
            negocioId?.let { putInt(KEY_NEGOCIO_ID, it) }
            negocioNombre?.let { putString(KEY_NEGOCIO_NOMBRE, it) }
        }.commit() // ✨ Usar commit() para asegurar escritura inmediata antes del sync
    }
    
    fun saveNegocioNombre(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_NEGOCIO_NOMBRE, name).apply()
    }

    fun getToken(context: Context): String? = getPrefs(context).getString(KEY_TOKEN, null)
    fun getNombre(context: Context): String? = getPrefs(context).getString(KEY_NOMBRE, null)
    fun getRol(context: Context): String? = getPrefs(context).getString(KEY_ROL, null)
    fun getNegocioId(context: Context): Int = getPrefs(context).getInt(KEY_NEGOCIO_ID, -1)
    fun getNegocioNombre(context: Context): String? = getPrefs(context).getString(KEY_NEGOCIO_NOMBRE, null)

    fun saveHojaRutaId(context: Context, hrId: Int) {
        getPrefs(context).edit().putInt(KEY_HOJA_RUTA_ID, hrId).apply()
    }
    fun getHojaRutaId(context: Context): Int = getPrefs(context).getInt(KEY_HOJA_RUTA_ID, -1)
    fun isLoggedIn(context: Context): Boolean = getToken(context) != null

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
