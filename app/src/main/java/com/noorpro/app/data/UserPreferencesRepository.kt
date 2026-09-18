package com.noorpro.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class LocationState(
    val latitude: Double,
    val longitude: Double,
    val isAvailable: Boolean
)

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.userPreferencesDataStore

    companion object {
        val LATITUDE_KEY = doublePreferencesKey("location_latitude")
        val LONGITUDE_KEY = doublePreferencesKey("location_longitude")
        val ALARM_SOUND_KEY = stringPreferencesKey("alarm_sound")
        val MONTHLY_PRAYER_GOAL = intPreferencesKey("monthly_prayer_goal")
        
        fun alarmEnabledKey(prayerName: String) = booleanPreferencesKey("alarm_enabled_$prayerName")
        fun alarmOffsetKey(prayerName: String) = intPreferencesKey("alarm_offset_$prayerName")
    }

    val monthlyPrayerGoalFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[MONTHLY_PRAYER_GOAL] ?: 150 // Default is 5 prayers * 30 days
    }

    suspend fun updateMonthlyPrayerGoal(goal: Int) {
        dataStore.edit { prefs ->
            prefs[MONTHLY_PRAYER_GOAL] = goal
        }
    }

    val locationFlow: Flow<LocationState> = dataStore.data.map { prefs ->
        val latitude = prefs[LATITUDE_KEY]
        val longitude = prefs[LONGITUDE_KEY]
        val isAvailable = latitude != null && longitude != null &&
            latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0
        LocationState(
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            isAvailable = isAvailable
        )
    }

    suspend fun updateLocation(lat: Double, lng: Double) {
        require(lat.isFinite() && lat in -90.0..90.0) { "Invalid latitude" }
        require(lng.isFinite() && lng in -180.0..180.0) { "Invalid longitude" }
        dataStore.edit { prefs ->
            prefs[LATITUDE_KEY] = lat
            prefs[LONGITUDE_KEY] = lng
        }
    }

    val alarmSoundFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[ALARM_SOUND_KEY] ?: "Mecca Adhan"
    }

    suspend fun updateAlarmSound(sound: String) {
        dataStore.edit { prefs ->
            prefs[ALARM_SOUND_KEY] = sound
        }
    }

    fun isAlarmEnabledFlow(prayerName: String): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[alarmEnabledKey(prayerName)] ?: false
    }

    suspend fun setAlarmEnabled(prayerName: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[alarmEnabledKey(prayerName)] = enabled
        }
    }

    fun alarmOffsetFlow(prayerName: String): Flow<Int> = dataStore.data.map { prefs ->
        prefs[alarmOffsetKey(prayerName)] ?: 10
    }

    suspend fun setAlarmOffset(prayerName: String, offsetMinutes: Int) {
        dataStore.edit { prefs ->
            prefs[alarmOffsetKey(prayerName)] = offsetMinutes
        }
    }

    fun getDhikrCountFlow(date: String, zikrName: String): Flow<Int> = dataStore.data.map { prefs ->
        prefs[intPreferencesKey("dhikr_${date}_$zikrName")] ?: 0
    }

    suspend fun updateDhikrCount(date: String, zikrName: String, count: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("dhikr_${date}_$zikrName")] = count
        }
    }

    fun getPdfBookmarkFlow(bookId: String): Flow<Int> = dataStore.data.map { prefs ->
        prefs[intPreferencesKey("bookmark_$bookId")] ?: 0
    }

    suspend fun savePdfBookmark(bookId: String, page: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("bookmark_$bookId")] = page
        }
    }
}
