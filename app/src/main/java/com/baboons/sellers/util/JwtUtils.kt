package com.baboons.sellers.util

import android.util.Base64
import org.json.JSONObject
import android.util.Log

object JwtUtils {
    fun decodeToken(token: String): Map<String, Any?> {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return emptyMap()
            
            // El payload es la segunda parte
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
            val json = JSONObject(payload)
            
            val map = mutableMapOf<String, Any?>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.opt(key)
                map[key] = if (value == JSONObject.NULL) null else value
            }
            map
        } catch (e: Exception) {
            Log.e("JwtUtils", "Error decodificando token", e)
            emptyMap()
        }
    }
}
