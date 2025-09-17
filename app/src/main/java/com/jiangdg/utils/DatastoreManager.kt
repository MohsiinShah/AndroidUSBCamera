package com.jiangdg.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class DatastoreManager(context: Context) {

    internal companion object {
        const val TAG = "DatastoreManager"
        const val DATA_STORE_NAME = "usb_monitor_datastore"
    }

    private val datastore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.applicationContext.preferencesDataStoreFile(DATA_STORE_NAME)
    }

    suspend fun saveData(key: String, value: Any?) {
        datastore.edit { preferences ->
            when (value) {
                is String -> {
                    preferences[stringPreferencesKey(key)] = value
                    Log.e(TAG, "$key String saved:$value")
                }

                is Int -> {
                    preferences[intPreferencesKey(key)] = value
                    Log.e(TAG, "$key Int saved:$value")
                }

                is Long -> {
                    preferences[longPreferencesKey(key)] = value
                    Log.e(TAG, "$key Long saved:$value")
                }

                is Float -> {
                    preferences[floatPreferencesKey(key)] = value
                    Log.e(TAG, "$key Float saved:$value")
                }

                is Boolean -> {
                    preferences[booleanPreferencesKey(key)] = value
                    Log.e(TAG, "$key Boolean saved:$value")
                }

                else -> throw kotlin.IllegalArgumentException("Unsupported preference type")
            }
        }
    }

    fun <T> getData(key: String, defaultValue: T): Flow<T> = flow {
        val preferencesKey = when (defaultValue) {
            is String -> stringPreferencesKey(key)
            is Int -> intPreferencesKey(key)
            is Long -> longPreferencesKey(key)
            is Float -> floatPreferencesKey(key)
            is Boolean -> booleanPreferencesKey(key)
            else -> throw kotlin.IllegalArgumentException("Unsupported preference type")
        }
        val preferences = datastore.data.first()
        val value = preferences[preferencesKey] ?: defaultValue
        emit(value as T)
    }.catch { exception ->
        if (exception is IOException) {
            emit(defaultValue)
        } else {
            throw exception
        }

    }

    suspend fun remove(key: String) {
        datastore.edit { preference ->
            preference.remove(stringPreferencesKey(key))
            preference.remove(intPreferencesKey(key))
            preference.remove(longPreferencesKey(key))
            preference.remove(floatPreferencesKey(key))
            preference.remove(booleanPreferencesKey(key))
        }
    }

    suspend fun clearData() {
        datastore.edit { preferences ->
            preferences.clear()
        }
    }

}