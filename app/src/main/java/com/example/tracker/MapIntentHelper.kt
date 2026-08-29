package com.example.tracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.model.GpsPoint

object MapIntentHelper {

    fun openInGoogleMaps(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String = "Current Workout Location"
    ) {
        try {
            val encodedLabel = Uri.encode(label)
            val uriString = "geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)&z=17"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to generic browser / maps url
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            try {
                val fallbackUri = Uri.parse("https://www.google.com/maps/@$latitude,$longitude,16z")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Unable to open Google Maps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openRouteInGoogleMaps(
        context: Context,
        points: List<GpsPoint>
    ) {
        if (points.isEmpty()) {
            Toast.makeText(context, "No GPS route points recorded yet", Toast.LENGTH_SHORT).show()
            return
        }

        val start = points.first()
        val end = points.last()

        try {
            val url = if (points.size > 1 && (start.latitude != end.latitude || start.longitude != end.longitude)) {
                "https://www.google.com/maps/dir/?api=1&origin=${start.latitude},${start.longitude}&destination=${end.latitude},${end.longitude}&travelmode=walking"
            } else {
                "https://www.google.com/maps/search/?api=1&query=${start.latitude},${start.longitude}"
            }

            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch Google Maps", Toast.LENGTH_SHORT).show()
        }
    }
}
