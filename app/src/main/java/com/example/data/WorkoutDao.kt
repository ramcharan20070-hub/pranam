package com.example.data

import androidx.room.*
import com.example.model.UserProfileEntity
import com.example.model.WorkoutCatalogEntity
import com.example.model.WorkoutSessionEntity
import com.example.model.WorkoutSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // --- Sessions ---
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM workout_sessions")
    fun getSessionCount(): Flow<Int>

    @Query("SELECT SUM(distanceMeters) FROM workout_sessions")
    fun getTotalDistanceMeters(): Flow<Double?>

    @Query("SELECT SUM(caloriesBurned) FROM workout_sessions")
    fun getTotalCaloriesBurned(): Flow<Int?>

    @Query("SELECT SUM(durationSeconds) FROM workout_sessions")
    fun getTotalDurationSeconds(): Flow<Long?>

    // --- User Profile ---
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT isSetupCompleted FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun isProfileSetupCompleted(): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    // --- Workout Catalog ---
    @Query("SELECT * FROM workouts")
    fun getAllWorkouts(): Flow<List<WorkoutCatalogEntity>>

    @Query("SELECT * FROM workouts WHERE workoutId = :id LIMIT 1")
    suspend fun getWorkoutById(id: Long): WorkoutCatalogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<WorkoutCatalogEntity>)

    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun getWorkoutsCount(): Int

    // --- Workout Suggestions ---
    @Query("SELECT * FROM workout_suggestions ORDER BY isCompleted ASC, dateRecommended DESC")
    fun getAllSuggestions(): Flow<List<WorkoutSuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestions(suggestions: List<WorkoutSuggestionEntity>)

    @Query("UPDATE workout_suggestions SET isCompleted = :isCompleted WHERE id = :suggestionId")
    suspend fun setSuggestionCompleted(suggestionId: Long, isCompleted: Boolean)

    @Query("DELETE FROM workout_suggestions WHERE isCompleted = 1")
    suspend fun clearCompletedSuggestions()
}
