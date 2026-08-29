package com.example.tracker

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.FileProvider
import com.example.ai.PostWorkoutAnalysis
import com.example.model.WorkoutSessionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object WorkoutReportGenerator {

    fun generateFormattedTextReport(
        session: WorkoutSessionEntity,
        analysis: PostWorkoutAnalysis?
    ): String {
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(session.startTime))
        val distKm = String.format(Locale.US, "%.2f km", session.distanceMeters / 1000.0)
        val paceFormatted = formatPace(session.avgPaceSecPerKm) + "/km"
        val durationFormatted = formatDuration(session.durationSeconds)

        val sb = StringBuilder()
        sb.append("⚡ PULSETRACK AI • WORKOUT SUMMARY ⚡\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🏃 Activity: ${session.workoutType.uppercase()}\n")
        sb.append("📅 Date: $dateStr\n\n")

        sb.append("📊 CORE TELEMETRY:\n")
        sb.append("• 📍 Distance: $distKm\n")
        sb.append("• ⏱️ Duration: $durationFormatted\n")
        sb.append("• ⚡ Avg Pace: $paceFormatted\n")
        sb.append("• 🔥 Active Burn: ${session.caloriesBurned} kcal\n")
        if (session.elevationGainMeters > 0) {
            sb.append("• ⛰️ Elevation Gain: +${session.elevationGainMeters.toInt()} m\n")
        }
        if (session.avgCadenceSpm > 0) {
            sb.append("• 👟 Avg Cadence: ${session.avgCadenceSpm} spm\n")
        }
        sb.append("\n")

        sb.append("❤️ BIOMETRICS & CARDIO:\n")
        sb.append("• Avg Heart Rate: ${session.avgHeartRate} BPM\n")
        sb.append("• Max Heart Rate: ${session.maxHeartRate} BPM\n\n")

        sb.append("🧠 AI PHYSIOLOGICAL ASSESSMENT:\n")
        sb.append("• AI Performance Score: ${session.aiPerformanceScore} / 100\n")
        sb.append("• Recovery Required: ${session.aiRecoveryHours} Hours\n")
        sb.append("• Training Stress: ${session.aiTrainingStress}\n\n")

        sb.append("💬 Coach's Feedback:\n")
        sb.append("\"${session.aiCoachingSummary}\"\n\n")

        if (analysis?.strengths?.isNotEmpty() == true) {
            sb.append("💪 Key Strengths:\n")
            analysis.strengths.forEach { s ->
                sb.append("  ✓ $s\n")
            }
            sb.append("\n")
        }

        if (analysis?.areasToImprove?.isNotEmpty() == true) {
            sb.append("🎯 Focus Next:\n")
            analysis.areasToImprove.forEach { s ->
                sb.append("  • $s\n")
            }
            sb.append("\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("#Pranam #FitnessTelemetry #SmartTracking #Cardio")

        return sb.toString()
    }

    fun generateWorkoutSnapshotBitmap(
        context: Context,
        session: WorkoutSessionEntity,
        analysis: PostWorkoutAnalysis?
    ): Bitmap {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Dark Futuristic Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.rgb(10, 15, 26),
                    Color.rgb(17, 24, 39),
                    Color.rgb(13, 19, 33)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Subtle Tech Grid Pattern
        val gridPaint = Paint().apply {
            color = Color.argb(20, 0, 240, 255)
            strokeWidth = 1.5f
        }
        for (i in 0..width step 90) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), gridPaint)
        }
        for (j in 0..height step 90) {
            canvas.drawLine(0f, j.toFloat(), width.toFloat(), j.toFloat(), gridPaint)
        }

        // 3. Glowing Cyber Outer Border
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.rgb(0, 240, 255)
        }
        val borderRect = RectF(24f, 24f, width - 24f, height - 24f)
        canvas.drawRoundRect(borderRect, 32f, 32f, borderPaint)

        // 4. Header Badge
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(0, 240, 255)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        canvas.drawText("PRANAM • PERFORMANCE SNAPSHOT", 64f, 90f, textPaint)

        // Workout Type Title
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${session.workoutType.uppercase()} SESSION", 64f, 165f, titlePaint)

        val datePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(148, 163, 184)
            textSize = 26f
        }
        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        canvas.drawText(dateFormat.format(Date(session.startTime)), 64f, 205f, datePaint)

        // 5. Circular AI Performance Score Badge (Top Right)
        val scoreCenterX = width - 160f
        val scoreCenterY = 145f
        val ringPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = Color.rgb(0, 255, 136)
        }
        canvas.drawCircle(scoreCenterX, scoreCenterY, 70f, ringPaint)

        val scoreTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(0, 255, 136)
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${session.aiPerformanceScore}", scoreCenterX, scoreCenterY + 12f, scoreTextPaint)

        val scoreSubPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(148, 163, 184)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("AI SCORE", scoreCenterX, scoreCenterY + 38f, scoreSubPaint)

        // 6. Primary 4-Metric Grid (Cards)
        val distKm = String.format(Locale.US, "%.2f", session.distanceMeters / 1000.0)
        val durationStr = formatDuration(session.durationSeconds)
        val paceStr = formatPace(session.avgPaceSecPerKm)
        val calsStr = "${session.caloriesBurned}"

        drawMetricCard(canvas, 64f, 260f, 440f, 150f, "DISTANCE", distKm, "KM", Color.rgb(0, 255, 136))
        drawMetricCard(canvas, 536f, 260f, 440f, 150f, "DURATION", durationStr, "", Color.rgb(0, 240, 255))
        drawMetricCard(canvas, 64f, 440f, 440f, 150f, "AVG PACE", paceStr, "/KM", Color.rgb(0, 209, 255))
        drawMetricCard(canvas, 536f, 440f, 440f, 150f, "CALORIES", calsStr, "KCAL", Color.rgb(255, 59, 107))

        // 7. Biometrics & Recovery Bar
        val bioCardRect = RectF(64f, 620f, width - 64f, 750f)
        val bioCardPaint = Paint().apply {
            color = Color.rgb(23, 32, 51)
        }
        val bioBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.rgb(51, 70, 100)
        }
        canvas.drawRoundRect(bioCardRect, 20f, 20f, bioCardPaint)
        canvas.drawRoundRect(bioCardRect, 20f, 20f, bioBorderPaint)

        val bioLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(0, 240, 255)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        canvas.drawText("HEART RATE & RECOVERY TELEMETRY", 90f, 660f, bioLabelPaint)

        val bioValPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Avg HR: ${session.avgHeartRate} BPM", 90f, 710f, bioValPaint)
        canvas.drawText("Max: ${session.maxHeartRate} BPM", 380f, 710f, bioValPaint)
        canvas.drawText("Recovery: ${session.aiRecoveryHours}h", 650f, 710f, bioValPaint)

        // 8. AI Tactical Summary Box
        val aiCardRect = RectF(64f, 780f, width - 64f, 960f)
        val aiCardPaint = Paint().apply {
            color = Color.rgb(21, 29, 44)
        }
        val aiBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.rgb(0, 240, 255)
        }
        canvas.drawRoundRect(aiCardRect, 20f, 20f, aiCardPaint)
        canvas.drawRoundRect(aiCardRect, 20f, 20f, aiBorderPaint)

        val aiTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(0, 240, 255)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("AI PHYSIOLOGICAL EVALUATION", 90f, 825f, aiTitlePaint)

        val aiSummaryPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(226, 232, 240)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val fullSummary = session.aiCoachingSummary
        val truncated = if (fullSummary.length > 130) fullSummary.substring(0, 127) + "..." else fullSummary
        canvas.drawText(truncated, 90f, 875f, aiSummaryPaint)

        val stressPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(255, 179, 0)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Training Stress: ${session.aiTrainingStress}  •  Recovery: ${session.aiRecoveryHours}h", 90f, 925f, stressPaint)

        // 9. Footer
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(100, 116, 139)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated by Pranam • Real-Time Biometric Coaching", width / 2f, 1020f, footerPaint)

        return bitmap
    }

    private fun drawMetricCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String,
        value: String,
        unit: String,
        accentColor: Int
    ) {
        val rect = RectF(x, y, x + w, y + h)
        val bgPaint = Paint().apply {
            color = Color.rgb(23, 32, 51)
        }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.rgb(45, 60, 85)
        }
        canvas.drawRoundRect(rect, 16f, 16f, bgPaint)
        canvas.drawRoundRect(rect, 16f, 16f, borderPaint)

        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(148, 163, 184)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        canvas.drawText(label, x + 24f, y + 42f, labelPaint)

        val valPaint = Paint().apply {
            isAntiAlias = true
            color = accentColor
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(value, x + 24f, y + 105f, valPaint)

        if (unit.isNotEmpty()) {
            val valWidth = valPaint.measureText(value)
            val unitPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(148, 163, 184)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(unit, x + 32f + valWidth, y + 105f, unitPaint)
        }
    }

    fun shareTextReport(context: Context, text: String, title: String = "Workout Summary") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Pranam • $title")
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Workout Report via")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    fun shareImageSnapshot(context: Context, bitmap: Bitmap, sessionTitle: String) {
        try {
            val cachePath = File(context.cacheDir, "shared_reports")
            cachePath.mkdirs()
            val file = File(cachePath, "workout_snapshot_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Pranam • $sessionTitle Snapshot")
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(shareIntent, "Share Workout Snapshot Card")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text share
            shareTextReport(context, "Workout Snapshot: $sessionTitle")
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    private fun formatPace(paceSecPerKm: Int): String {
        if (paceSecPerKm <= 0 || paceSecPerKm > 1800) return "--:--"
        val mins = paceSecPerKm / 60
        val secs = paceSecPerKm % 60
        return String.format(Locale.US, "%d:%02d", mins, secs)
    }
}
