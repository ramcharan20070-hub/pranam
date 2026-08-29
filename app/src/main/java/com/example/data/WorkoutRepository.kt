package com.example.data

import com.example.model.UserProfileEntity
import com.example.model.WorkoutCatalogEntity
import com.example.model.WorkoutSessionEntity
import com.example.model.WorkoutSuggestionEntity
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val dao: WorkoutDao) {

    val allSessions: Flow<List<WorkoutSessionEntity>> = dao.getAllSessions()
    val sessionCount: Flow<Int> = dao.getSessionCount()
    val totalDistanceMeters: Flow<Double?> = dao.getTotalDistanceMeters()
    val totalCaloriesBurned: Flow<Int?> = dao.getTotalCaloriesBurned()
    val totalDurationSeconds: Flow<Long?> = dao.getTotalDurationSeconds()
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val allSuggestions: Flow<List<WorkoutSuggestionEntity>> = dao.getAllSuggestions()
    val allWorkouts: Flow<List<WorkoutCatalogEntity>> = dao.getAllWorkouts()

    suspend fun getSessionById(sessionId: Long): WorkoutSessionEntity? {
        return dao.getSessionById(sessionId)
    }

    suspend fun saveSession(session: WorkoutSessionEntity): Long {
        return dao.insertSession(session)
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteSession(sessionId)
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        dao.insertOrUpdateProfile(profile)
    }

    suspend fun saveSuggestions(suggestions: List<WorkoutSuggestionEntity>) {
        dao.insertSuggestions(suggestions)
    }

    suspend fun setSuggestionCompleted(suggestionId: Long, isCompleted: Boolean) {
        dao.setSuggestionCompleted(suggestionId, isCompleted)
    }

    suspend fun ensureWorkoutsSeeded() {
        if (dao.getWorkoutsCount() == 0) {
            AppDatabase.populateInitialData(dao)
        }
    }
}
