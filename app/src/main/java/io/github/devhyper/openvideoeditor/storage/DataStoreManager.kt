package io.github.devhyper.openvideoeditor.storage

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Using an extension on Context to initialize DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

@SuppressLint("StaticFieldLeak")
object DataStoreManager {
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private val FLOAT_KEY = floatPreferencesKey("float_key")
    private val INT_KEY = intPreferencesKey("int_key")
    private val STRING_KEY = stringPreferencesKey("string_key")
    private val BOOLEAN_KEY = booleanPreferencesKey("boolean_key")

    // Function to save a float
    suspend fun saveFloat(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[FLOAT_KEY] = value
        }
    }

    // Function to read a float
    val readFloat: Flow<Float?> = context.dataStore.data
        .map { preferences ->
            preferences[FLOAT_KEY]
        }

    // Function to save an int
    suspend fun saveInt(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[INT_KEY] = value
        }
    }

    // Function to read an int
    val readInt: Flow<Int?> = context.dataStore.data
        .map { preferences ->
            preferences[INT_KEY]
        }

    // Function to save a string
    suspend fun saveString(value: String) {
        context.dataStore.edit { preferences ->
            preferences[STRING_KEY] = value
        }
    }

    // Function to read a string
    val readString: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[STRING_KEY]
        }

    // Function to save a boolean
    suspend fun saveBoolean(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BOOLEAN_KEY] = value
        }
    }

    // Function to read a boolean
    val readBoolean: Flow<Boolean?> = context.dataStore.data
        .map { preferences ->
            preferences[BOOLEAN_KEY]
        }
}




