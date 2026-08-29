package com.example.tracker

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object LocationAddressResolver {

    private val addressCache = ConcurrentHashMap<String, String>()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) {
            return@withContext "Location pending..."
        }

        val cacheKey = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
        addressCache[cacheKey]?.let { return@withContext it }

        // 1. Try Android Native Geocoder
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var resolvedName: String? = null
                    val lock = java.util.concurrent.CountDownLatch(1)
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (addresses.isNotEmpty()) {
                                resolvedName = formatAddress(addresses[0])
                            }
                            lock.countDown()
                        }

                        override fun onError(errorMessage: String?) {
                            lock.countDown()
                        }
                    })
                    lock.await(2, TimeUnit.SECONDS)
                    if (!resolvedName.isNullOrBlank()) {
                        addressCache[cacheKey] = resolvedName!!
                        return@withContext resolvedName!!
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val formatted = formatAddress(addresses[0])
                        if (formatted.isNotBlank()) {
                            addressCache[cacheKey] = formatted
                            return@withContext formatted
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallthrough to web reverse geocode
            }
        }

        // 2. High-Accuracy Web Reverse Geocoding Fallback (OpenStreetMap Nominatim)
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json&zoom=18&addressdetails=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PranamFitnessApp/2.0 (Android AI Studio)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val addressObj = json.optJSONObject("address")
                        if (addressObj != null) {
                            val road = addressObj.optString("road", "")
                            val houseNumber = addressObj.optString("house_number", "")
                            val suburb = addressObj.optString("suburb", addressObj.optString("neighbourhood", ""))
                            val city = addressObj.optString("city", addressObj.optString("town", addressObj.optString("county", "")))
                            val park = addressObj.optString("leisure", addressObj.optString("amenity", ""))

                            val streetPart = when {
                                road.isNotBlank() && houseNumber.isNotBlank() -> "$houseNumber $road"
                                road.isNotBlank() -> road
                                park.isNotBlank() -> park
                                else -> ""
                            }

                            val areaPart = when {
                                suburb.isNotBlank() && city.isNotBlank() && suburb != city -> "$suburb, $city"
                                suburb.isNotBlank() -> suburb
                                city.isNotBlank() -> city
                                else -> ""
                            }

                            val formatted = when {
                                streetPart.isNotBlank() && areaPart.isNotBlank() -> "$streetPart, $areaPart"
                                streetPart.isNotBlank() -> streetPart
                                areaPart.isNotBlank() -> areaPart
                                else -> json.optString("display_name", "")
                            }

                            if (formatted.isNotBlank()) {
                                val clean = if (formatted.length > 50) formatted.take(48) + "..." else formatted
                                addressCache[cacheKey] = clean
                                return@withContext clean
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallthrough to formatted coordinates
        }

        // 3. Formatted High-Precision Coordinate Fallback
        val latDir = if (latitude >= 0) "N" else "S"
        val lngDir = if (longitude >= 0) "E" else "W"
        val fallback = String.format(Locale.US, "%.4f° %s, %.4f° %s", kotlin.math.abs(latitude), latDir, kotlin.math.abs(longitude), lngDir)
        addressCache[cacheKey] = fallback
        fallback
    }

    private fun formatAddress(address: Address): String {
        val feature = address.featureName
        val thoroughfare = address.thoroughfare
        val subLocality = address.subLocality
        val locality = address.locality ?: address.subAdminArea ?: address.adminArea

        return when {
            !thoroughfare.isNullOrBlank() && !locality.isNullOrBlank() -> {
                val street = if (!feature.isNullOrBlank() && feature != thoroughfare && !feature.matches(Regex("\\d+.*"))) {
                    "$feature, $thoroughfare"
                } else if (!feature.isNullOrBlank() && feature.matches(Regex("\\d+.*"))) {
                    "$feature $thoroughfare"
                } else {
                    thoroughfare
                }
                "$street, $locality"
            }
            !subLocality.isNullOrBlank() && !locality.isNullOrBlank() -> "$subLocality, $locality"
            !locality.isNullOrBlank() -> locality
            address.maxAddressLineIndex >= 0 -> address.getAddressLine(0) ?: "Workout Location"
            else -> "Workout Location"
        }
    }
}
