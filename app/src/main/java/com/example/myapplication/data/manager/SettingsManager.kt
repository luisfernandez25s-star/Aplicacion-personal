package com.example.myapplication.data.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val MONGODB_URI_KEY = stringPreferencesKey("mongodb_uri")
    }

    val mongoUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[MONGODB_URI_KEY] ?: "mongodb://luisg:gb22x4hLFjueWDKo@escuela-shard-00-00.tjez8ct.mongodb.net:27017,escuela-shard-00-01.tjez8ct.mongodb.net:27017,escuela-shard-00-02.tjez8ct.mongodb.net:27017/Sensores?authSource=admin&tls=true&retryWrites=true&w=majority&connectTimeoutMS=20000&serverSelectionTimeoutMS=20000"
    }

    suspend fun saveMongoUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[MONGODB_URI_KEY] = uri
        }
    }
}
