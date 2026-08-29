package com.example.ai

import com.example.BuildConfig
import com.example.model.LiveBiometricState
import com.example.model.UserProfileEntity
import com.example.model.WorkoutSessionEntity
import com.example.model.WorkoutSuggestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PostWorkoutAnalysis(
    val performanceScore: Int,
    val trainingStress: String,
    val recoveryHours: Int,
    val coachingSummary: String,
    val strengths: List<String>,
    val areasToImprove: List<String>
)

class GeminiFitnessService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun callGeminiApi(prompt: String, systemInstruction: String? = null): String =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext "API key not configured. Please set GEMINI_API_KEY in Secrets."
            }

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            if (!systemInstruction.isNullOrBlank()) {
                val sysContent = JSONObject()
                val sysParts = JSONArray()
                val sysPart = JSONObject()
                sysPart.put("text", systemInstruction)
                sysParts.put(sysPart)
                sysContent.put("parts", sysParts)
                rootJson.put("systemInstruction", sysContent)
            }

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext "AI Service Notice: HTTP ${response.code}"
                    }
                    val respObj = JSONObject(bodyString)
                    val candidates = respObj.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text") ?: ""
                    text.trim()
                }
            } catch (e: Exception) {
                "AI Feedback unavailable offline: ${e.localizedMessage ?: "Connection error"}"
            }
        }

    suspend fun getLiveBiometricCoaching(
        state: LiveBiometricState,
        profile: UserProfileEntity
    ): String {
        val paceFormatted = if (state.avgPaceSecPerKm > 0) {
            val m = state.avgPaceSecPerKm / 60
            val s = state.avgPaceSecPerKm % 60
            String.format("%d:%02d /km", m, s)
        } else "0:00 /km"

        val activityBiomechanicalFocus = when (state.workoutType) {
            com.example.model.WorkoutType.RUNNING -> "Biomechanical focus: Cadence rhythm (target ~170-180 SPM), foot strike under center of mass, upright torso, relaxed shoulders, and aerobic efficiency."
            com.example.model.WorkoutType.CYCLING -> "Biomechanical focus: Pedal stroke smoothness (target 85-95 RPM), power-to-HR ratio, aerodynamic positioning, and pacing on gradients."
            com.example.model.WorkoutType.TRAIL_RUN -> "Biomechanical focus: Trail navigation, short responsive strides on ascents, core stability, grade-adjusted effort, and eccentric downhill control."
            com.example.model.WorkoutType.HIIT_SPRINT -> "Biomechanical focus: Maximum explosive power drive, hip extension, rapid interval recovery, and lactate buffer control."
            com.example.model.WorkoutType.POWER_WALK -> "Biomechanical focus: Active heel-to-toe roll, 90-degree arm swing cadence, posture alignment, and steady fat oxidation heart rate."
            com.example.model.WorkoutType.HIKING -> "Biomechanical focus: Sustained low-cadence climbing endurance, breathing cadence synchronization, and joint stability on elevation."
            com.example.model.WorkoutType.ROWING -> "Biomechanical focus: 1:2 drive-to-recovery rhythm, core bracing at catch, smooth sequential drive (legs-core-arms), and stroke rate control."
        }

        val prompt = """
            You are a real-time elite Olympic sports performance coach analyzing live biometric telemetry during a workout.
            Athlete Profile: Age ${profile.age}, Goal: ${profile.fitnessGoal}, Experience: ${profile.experienceLevel}.
            Live Telemetry:
            - Activity: ${state.workoutType.title}
            - Location Context: Started at ${state.startLocationName.ifEmpty { "Outdoor route" }}
            - Elapsed Time: ${state.durationSeconds / 60}m ${state.durationSeconds % 60}s
            - Distance: ${String.format("%.2f", state.distanceMeters / 1000.0)} km
            - Current Heart Rate: ${state.currentHeartRate} BPM (${state.currentZone.title} - Zone ${state.currentZone.zoneNumber})
            - Average HR: ${state.avgHeartRate} BPM, Max HR: ${state.maxHeartRate} BPM
            - Cadence: ${state.currentCadenceSpm} SPM (Avg: ${state.avgCadenceSpm} SPM)
            - Current Pace: $paceFormatted
            - Fatigue Level: ${state.fatigueScore}/100, Stamina Remaining: ${state.staminaPercent}%
            - Elevation Gain: ${String.format("%.1f", state.elevationGainMeters)}m
            
            $activityBiomechanicalFocus
            
            Task: Give 1-2 sentences of ultra-concise, immediate, activity-personalized athletic guidance tailored specifically to ${state.workoutType.title}. Be direct, energizing, and scientifically actionable.
        """.trimIndent()

        val sysPrompt = "You are an elite high-tech biometric sports coach giving crisp, 1-2 sentence real-time tactical instructions tailored specifically to the user's active sport."
        val result = callGeminiApi(prompt, sysPrompt)
        return if (result.startsWith("AI Feedback unavailable") || result.startsWith("API key not configured")) {
            generateFallbackLiveTip(state)
        } else {
            result
        }
    }

    suspend fun generatePostWorkoutDeepDive(
        session: WorkoutSessionEntity,
        profile: UserProfileEntity
    ): PostWorkoutAnalysis {
        val paceMin = session.avgPaceSecPerKm / 60
        val paceSec = session.avgPaceSecPerKm % 60
        val distKm = session.distanceMeters / 1000.0
        val durMin = session.durationSeconds / 60

        val prompt = """
            Analyze this completed workout session and return a structured athletic performance report.
            Athlete: Age ${profile.age}, Goal: ${profile.fitnessGoal}.
            Session Stats:
            - Activity: ${session.workoutType}
            - Start Location: ${session.startLocationName}
            - End Location: ${session.endLocationName}
            - Duration: ${durMin} min
            - Distance: ${String.format("%.2f", distKm)} km
            - Calories: ${session.caloriesBurned} kcal
            - Avg HR: ${session.avgHeartRate} BPM, Max HR: ${session.maxHeartRate} BPM
            - Avg Pace: ${paceMin}:${String.format("%02d", paceSec)} /km
            - Avg Cadence: ${session.avgCadenceSpm} SPM
            - Elevation Gain: ${String.format("%.1f", session.elevationGainMeters)}m

            Please respond in the following EXACT JSON format:
            {
              "performanceScore": 88,
              "trainingStress": "Optimal Stimulus",
              "recoveryHours": 24,
              "coachingSummary": "Excellent aerobic foundation work tailored to ${session.workoutType} from ${session.startLocationName} to ${session.endLocationName}.",
              "strengths": ["Consistent cadence discipline", "Solid aerobic zone endurance"],
              "areasToImprove": ["Slight cardiac drift in final 10 minutes", "Increase cadence uphill"]
            }
        """.trimIndent()

        val sysPrompt = "You are a professional exercise physiologist and sports data scientist providing JSON workout evaluations."
        val rawResponse = callGeminiApi(prompt, sysPrompt)

        return try {
            val cleanJson = rawResponse.substringAfter("{", "").substringBeforeLast("}", "")
            if (cleanJson.isNotEmpty()) {
                val fullJson = JSONObject("{$cleanJson}")
                val score = fullJson.optInt("performanceScore", 85).coerceIn(40, 99)
                val stress = fullJson.optString("trainingStress", "Optimal Stimulus")
                val recovery = fullJson.optInt("recoveryHours", 24).coerceIn(6, 72)
                val summary = fullJson.optString("coachingSummary", "Great workout session with strong pacing control.")
                val strengths = mutableListOf<String>()
                val areas = mutableListOf<String>()

                val strArray = fullJson.optJSONArray("strengths")
                if (strArray != null) {
                    for (i in 0 until strArray.length()) strengths.add(strArray.getString(i))
                }
                val areaArray = fullJson.optJSONArray("areasToImprove")
                if (areaArray != null) {
                    for (i in 0 until areaArray.length()) areas.add(areaArray.getString(i))
                }

                PostWorkoutAnalysis(
                    performanceScore = score,
                    trainingStress = stress,
                    recoveryHours = recovery,
                    coachingSummary = summary,
                    strengths = if (strengths.isEmpty()) listOf("Consistent pace pacing", "Heart rate control") else strengths,
                    areasToImprove = if (areas.isEmpty()) listOf("Hydration and mineral timing") else areas
                )
            } else {
                generateFallbackPostAnalysis(session)
            }
        } catch (e: Exception) {
            generateFallbackPostAnalysis(session)
        }
    }

    suspend fun generatePersonalizedSuggestions(
        profile: UserProfileEntity,
        recentSessions: List<WorkoutSessionEntity>
    ): List<WorkoutSuggestionEntity> {
        val prompt = """
            Based on the user's profile and recent session volume, generate 3 tailored future workout suggestions.
            Profile: Age ${profile.age}, Weight ${profile.weightKg}kg, Goal: ${profile.fitnessGoal}, Level: ${profile.experienceLevel}.
            Recent Sessions Count: ${recentSessions.size}. Total recent distance: ${recentSessions.sumOf { it.distanceMeters } / 1000.0} km.

            Provide 3 workouts in this JSON structure:
            [
              {
                "title": "Aerobic Threshold Progression",
                "workoutType": "RUNNING",
                "targetDurationMin": 45,
                "targetZone": "Zone 3 (Aerobic)",
                "intensity": "Moderate Aerobic",
                "rationale": "Strengthen aerobic threshold and fat oxidation efficiency.",
                "recommendedIntervals": "10 min Z1 warm up, 25 min Z3 steady tempo, 10 min Z1 cooldown"
              }
            ]
        """.trimIndent()

        val sysPrompt = "You are an elite athletic coach generating JSON personalized workout recommendations."
        val rawResponse = callGeminiApi(prompt, sysPrompt)

        return try {
            val startIdx = rawResponse.indexOf("[")
            val endIdx = rawResponse.lastIndexOf("]")
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                val jsonArr = JSONArray(rawResponse.substring(startIdx, endIdx + 1))
                val resultList = mutableListOf<WorkoutSuggestionEntity>()
                for (i in 0 until jsonArr.length()) {
                    val item = jsonArr.getJSONObject(i)
                    resultList.add(
                        WorkoutSuggestionEntity(
                            title = item.optString("title", "Structured Cardio Session"),
                            workoutType = item.optString("workoutType", "RUNNING"),
                            targetDurationMin = item.optInt("targetDurationMin", 35),
                            targetZone = item.optString("targetZone", "Zone 2 (Fat Burn)"),
                            intensity = item.optString("intensity", "Moderate Aerobic"),
                            rationale = item.optString("rationale", "Personalized based on recent biometric training load."),
                            recommendedIntervals = item.optString("recommendedIntervals", "Standard progression intervals")
                        )
                    )
                }
                if (resultList.isNotEmpty()) resultList else getFallbackSuggestions(profile)
            } else {
                getFallbackSuggestions(profile)
            }
        } catch (e: Exception) {
            getFallbackSuggestions(profile)
        }
    }

    suspend fun askCoach(
        question: String,
        profile: UserProfileEntity,
        latestSession: WorkoutSessionEntity?
    ): String {
        val sessionContext = if (latestSession != null) {
            "Latest session: ${latestSession.workoutType}, ${(latestSession.distanceMeters / 1000.0).toInt()}km, Avg HR ${latestSession.avgHeartRate} BPM, Score ${latestSession.aiPerformanceScore}/100."
        } else "No recent session recorded."

        val prompt = """
            Athlete Question: "$question"
            Athlete Info: Age ${profile.age}, Goal: ${profile.fitnessGoal}, Level: ${profile.experienceLevel}.
            Context: $sessionContext
            
            Provide a knowledgeable, scientifically grounded, encouraging athletic coach answer in 2-3 clear paragraphs with bullet points for action items where applicable.
        """.trimIndent()

        val sysPrompt = "You are Pranam AI, an Olympic-caliber digital sports physiologist and training consultant."
        return callGeminiApi(prompt, sysPrompt)
    }

    private fun generateFallbackLiveTip(state: LiveBiometricState): String {
        return when (state.currentZone.zoneNumber) {
            1 -> "Active warm-up phase. Maintain fluid breathing and relaxed shoulders as your cardiovascular system primes."
            2 -> "Optimal fat oxidation zone. Keep cadence steady around ${state.workoutType.typicalCadence} SPM for sustained aerobic efficiency."
            3 -> "Solid aerobic tempo. Engage core muscles and drive through your glutes to maintain split consistency."
            4 -> "Approaching lactate threshold. Control exhalations and focus on relaxed arm swing to manage lactate accumulation."
            5 -> "Peak VO2 Max intensity! Limit duration in this zone to 2-3 minute bursts and prepare for active recovery."
            else -> "Maintain consistent cadence and posture for optimal biomechanical efficiency."
        }
    }

    private fun generateFallbackPostAnalysis(session: WorkoutSessionEntity): PostWorkoutAnalysis {
        val distKm = session.distanceMeters / 1000.0
        val durMin = session.durationSeconds / 60
        val paceMin = session.avgPaceSecPerKm / 60
        val paceSec = session.avgPaceSecPerKm % 60
        val paceStr = if (session.avgPaceSecPerKm > 0) "${paceMin}:${String.format("%02d", paceSec)} /km" else "N/A"

        val score = (84 + ((session.durationSeconds + session.caloriesBurned) % 13)).toInt().coerceIn(75, 98)
        val recovery = when {
            session.durationSeconds > 4500 || session.caloriesBurned > 600 -> 36
            session.durationSeconds > 2400 || session.caloriesBurned > 350 -> 24
            session.durationSeconds > 1200 -> 16
            else -> 8
        }

        val activityTypeTitle = session.workoutType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        val locationClause = if (session.startLocationName.isNotBlank() && session.endLocationName.isNotBlank() && session.startLocationName != session.endLocationName) {
            " traversed from ${session.startLocationName} to ${session.endLocationName}"
        } else if (session.startLocationName.isNotBlank()) {
            " around ${session.startLocationName}"
        } else ""

        val summary = "Completed a high-quality ${String.format("%.2f", distKm)} km $activityTypeTitle session in ${durMin}m ($paceStr)$locationClause. Maintained an average heart rate of ${session.avgHeartRate} BPM (peak ${session.maxHeartRate} BPM) with strong metabolic burn (${session.caloriesBurned} kcal) and consistent cadence rhythm of ${session.avgCadenceSpm} SPM."

        val strengths = mutableListOf<String>()
        val areasToImprove = mutableListOf<String>()

        if (session.avgCadenceSpm in 160..195) {
            strengths.add("Excellent cadence discipline (${session.avgCadenceSpm} SPM)")
        } else {
            strengths.add("Solid continuous aerobic output")
        }

        if (session.avgHeartRate in 120..165) {
            strengths.add("Controlled aerobic zone pacing")
        } else if (session.avgHeartRate > 165) {
            strengths.add("High anaerobic threshold conditioning")
        } else {
            strengths.add("Regenerative active recovery efficiency")
        }

        if (session.elevationGainMeters > 20.0) {
            strengths.add("Strong hill climbing resilience (+${String.format("%.0f", session.elevationGainMeters)}m elevation)")
        } else {
            strengths.add("Smooth split pace consistency")
        }

        if (session.avgHeartRate > 170) {
            areasToImprove.add("Incorporate active recovery intervals to prevent cardiac fatigue")
        } else {
            areasToImprove.add("Hydrate with electrolytes within 30 minutes post-session")
        }
        areasToImprove.add("Perform 5 minutes of lower limb dynamic stretching")

        return PostWorkoutAnalysis(
            performanceScore = score,
            trainingStress = if (session.avgHeartRate > 168) "High Anaerobic Load" else if (session.avgHeartRate > 145) "Optimal Aerobic Stimulus" else "Aerobic Base / Active Recovery",
            recoveryHours = recovery,
            coachingSummary = summary,
            strengths = strengths,
            areasToImprove = areasToImprove
        )
    }

    private fun getFallbackSuggestions(profile: UserProfileEntity): List<WorkoutSuggestionEntity> {
        return listOf(
            WorkoutSuggestionEntity(
                title = "Zone 2 Mitochondrial Endurance",
                workoutType = "RUNNING",
                targetDurationMin = 40,
                targetZone = "Zone 2 (Fat Burn)",
                intensity = "Moderate Aerobic",
                rationale = "Targeted aerobic stimulus to expand stroke volume and base metabolic efficiency for ${profile.fitnessGoal}.",
                recommendedIntervals = "5 min warm-up @ 120 BPM, 30 min steady @ 132-145 BPM, 5 min cool-down"
            ),
            WorkoutSuggestionEntity(
                title = "High Cadence Tempo Surges",
                workoutType = "CYCLING",
                targetDurationMin = 45,
                targetZone = "Zone 3 (Aerobic)",
                intensity = "Threshold",
                rationale = "Develop neuromuscular cadence efficiency and muscular stamina.",
                recommendedIntervals = "10 min warm-up, 5x (3 min high-cadence 95+ RPM / 2 min recovery), 10 min cool-down"
            ),
            WorkoutSuggestionEntity(
                title = "Regenerative Recovery & Mobility Walk",
                workoutType = "POWER_WALK",
                targetDurationMin = 25,
                targetZone = "Zone 1 (Warm Up)",
                intensity = "Low Recovery",
                rationale = "Promote circulation and reduce delayed-onset muscle soreness.",
                recommendedIntervals = "Continuous relaxed pace, nasal breathing only, heart rate < 118 BPM"
            )
        )
    }
}
