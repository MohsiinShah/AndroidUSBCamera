package com.jiangdg.module

import android.app.Application
import android.content.Context
import com.jiangdg.utils.DatastoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    /**---------------------------- App Context ----------------------------*/

    @Provides
    fun provideContext(application: Application): Context = application.applicationContext


    @Singleton
    @Provides
    fun providesDatastoreManager(context: Context): DatastoreManager {
        return DatastoreManager(context = context)
    }

}