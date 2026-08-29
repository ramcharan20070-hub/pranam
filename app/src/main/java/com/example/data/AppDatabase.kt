package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.UserProfileEntity
import com.example.model.WorkoutCatalogEntity
import com.example.model.WorkoutSessionEntity
import com.example.model.WorkoutSuggestionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WorkoutSessionEntity::class,
        UserProfileEntity::class,
        WorkoutSuggestionEntity::class,
        WorkoutCatalogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pulsetrack_fitness_db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate initial profile and suggestions
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.workoutDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: WorkoutDao) {
            val existingProfile = dao.getUserProfile()
            dao.insertOrUpdateProfile(UserProfileEntity(isSetupCompleted = false))
            
            val initialWorkouts = listOf(
                WorkoutCatalogEntity(
                    workoutId = 1L,
                    workoutName = "Full Body Beginner",
                    difficulty = "Beginner",
                    goal = "Improve Fitness",
                    equipment = "No Equipment",
                    durationMin = 20,
                    caloriesEst = 140,
                    workoutType = "POWER_WALK",
                    description = "Gentle full-body movement focusing on joint mobility, light cardio, and base metabolic conditioning.",
                    targetZones = "Zone 1 (Warm Up) & Zone 2",
                    intervals = "3 min gentle warmup, 14 min continuous brisk walk + calisthenics, 3 min stretch"
                ),
                WorkoutCatalogEntity(
                    workoutId = 2L,
                    workoutName = "Home Fat Burn HIIT",
                    difficulty = "Intermediate",
                    goal = "Weight Loss",
                    equipment = "No Equipment",
                    durationMin = 20,
                    caloriesEst = 220,
                    workoutType = "HIIT_SPRINT",
                    description = "High energy bodyweight intervals engineered for post-exercise oxygen consumption (EPOC) and rapid caloric expenditure.",
                    targetZones = "Zone 3 (Aerobic) & Zone 4 (Anaerobic)",
                    intervals = "3 min warmup, 6 rounds (45s sprint/burpees / 30s active rest), 2 min cooldown"
                ),
                WorkoutCatalogEntity(
                    workoutId = 3L,
                    workoutName = "Upper Body Dumbbell Strength",
                    difficulty = "Intermediate",
                    goal = "Increase Strength",
                    equipment = "Dumbbells",
                    durationMin = 30,
                    caloriesEst = 210,
                    workoutType = "ROWING",
                    description = "Hypertrophy-focused upper body compound movements targeting back, shoulders, chest, and arms.",
                    targetZones = "Zone 2 & Zone 3",
                    intervals = "4 min arm circles/warmup, 4x sets rows & overhead presses with 60s rest, 3 min cooldown"
                ),
                WorkoutCatalogEntity(
                    workoutId = 4L,
                    workoutName = "Lower Body Bodyweight Strength",
                    difficulty = "Beginner",
                    goal = "Increase Strength",
                    equipment = "No Equipment",
                    durationMin = 25,
                    caloriesEst = 175,
                    workoutType = "POWER_WALK",
                    description = "Glute, quad, and hamstring foundational endurance without knee strain or complex weights.",
                    targetZones = "Zone 2 (Fat Burn)",
                    intervals = "3 min mobility, 3x sets squats, glute bridges, reverse lunges, 4 min stretch"
                ),
                WorkoutCatalogEntity(
                    workoutId = 5L,
                    workoutName = "Full Body Muscle Hypertrophy",
                    difficulty = "Advanced",
                    goal = "Gain Muscle",
                    equipment = "Dumbbells",
                    durationMin = 45,
                    caloriesEst = 340,
                    workoutType = "RUNNING",
                    description = "High volume total-body resistance session maximizing time-under-tension and metabolic fatigue.",
                    targetZones = "Zone 3 & Zone 4",
                    intervals = "5 min dynamic warmup, 5 supersets dumbbell thrusters, renegade rows, lunges, 5 min cooldown"
                ),
                WorkoutCatalogEntity(
                    workoutId = 6L,
                    workoutName = "Low-Impact Aerobic Base",
                    difficulty = "Beginner",
                    goal = "Weight Loss",
                    equipment = "No Equipment",
                    durationMin = 30,
                    caloriesEst = 190,
                    workoutType = "POWER_WALK",
                    description = "Steady Zone 2 fat-burning cardio that protects joints while steadily building mitochondrial density.",
                    targetZones = "Zone 2 (Fat Burn)",
                    intervals = "5 min warm-up, 20 min steady rhythmic walk at 115-130 BPM, 5 min cooldown"
                ),
                WorkoutCatalogEntity(
                    workoutId = 7L,
                    workoutName = "Cardio Stamina & Endurance Run",
                    difficulty = "Intermediate",
                    goal = "Improve Endurance",
                    equipment = "No Equipment",
                    durationMin = 45,
                    caloriesEst = 420,
                    workoutType = "RUNNING",
                    description = "Progressive pacing run to expand aerobic threshold and cardiovascular endurance.",
                    targetZones = "Zone 3 (Aerobic) & Zone 4",
                    intervals = "5 min jog, 35 min steady aerobic tempo at 150-165 BPM, 5 min recovery walk"
                ),
                WorkoutCatalogEntity(
                    workoutId = 8L,
                    workoutName = "Active Daily Mobility & Recovery",
                    difficulty = "Beginner",
                    goal = "Stay Active",
                    equipment = "No Equipment",
                    durationMin = 15,
                    caloriesEst = 80,
                    workoutType = "POWER_WALK",
                    description = "Gentle active recovery promoting circulation, posture alignment, and stress reduction.",
                    targetZones = "Zone 1 (Warm Up)",
                    intervals = "15 min gentle walking and dynamic range of motion stretches"
                ),
                WorkoutCatalogEntity(
                    workoutId = 9L,
                    workoutName = "Full Gym Power & Conditioning",
                    difficulty = "Advanced",
                    goal = "Gain Muscle",
                    equipment = "Full Gym",
                    durationMin = 60,
                    caloriesEst = 520,
                    workoutType = "ROWING",
                    description = "Comprehensive gym circuit combining barbell squats, rowing ergometer intervals, and cable rotations.",
                    targetZones = "Zone 3 & Zone 4",
                    intervals = "8 min warmup, 4x power circuits with 90s rest, 8 min recovery"
                ),
                WorkoutCatalogEntity(
                    workoutId = 10L,
                    workoutName = "Resistance Bands Core & Tone",
                    difficulty = "Intermediate",
                    goal = "Improve Fitness",
                    equipment = "Resistance Bands",
                    durationMin = 30,
                    caloriesEst = 210,
                    workoutType = "CYCLING",
                    description = "Targeted elastic resistance exercises for core stabilization, postural strength, and lean tone.",
                    targetZones = "Zone 2 & Zone 3",
                    intervals = "4 min warmup, 4 rounds banded pulls, paloff presses, banded squats, 3 min cooldown"
                )
            )
            dao.insertWorkouts(initialWorkouts)

            val initialSuggestions = listOf(
                WorkoutSuggestionEntity(
                    title = "Aerobic Base Building",
                    workoutType = "RUNNING",
                    targetDurationMin = 35,
                    targetZone = "Zone 2 (Fat Burn)",
                    intensity = "Moderate Aerobic",
                    rationale = "Biometric analysis indicates high recovery status. Expand mitochondrial capacity with steady Zone 2 cardio.",
                    recommendedIntervals = "5 min warm-up @ Zone 1, 25 min steady @ 135-145 BPM, 5 min cool-down"
                ),
                WorkoutSuggestionEntity(
                    title = "Lactate Threshold Intervals",
                    workoutType = "CYCLING",
                    targetDurationMin = 40,
                    targetZone = "Zone 4 (Anaerobic)",
                    intensity = "High Intensity",
                    rationale = "Elevate lactate clearance threshold to maintain faster average speeds over extended distances.",
                    recommendedIntervals = "10 min warm-up, 4x 4-minute surges in Zone 4 (88-92% HR Max) with 2 min active recovery, 6 min cool-down"
                ),
                WorkoutSuggestionEntity(
                    title = "Active Biometric Recovery Walk",
                    workoutType = "POWER_WALK",
                    targetDurationMin = 25,
                    targetZone = "Zone 1 (Warm Up)",
                    intensity = "Low Recovery",
                    rationale = "Promote lymphatic drainage and muscular repair without accumulating central nervous system fatigue.",
                    recommendedIntervals = "Continuous low-intensity stride, maintaining HR under 120 BPM with steady rhythmic breathing"
                )
            )
            dao.insertSuggestions(initialSuggestions)
        }
    }
}
