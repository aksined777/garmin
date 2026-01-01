package com.example.garmin.di

import android.content.Context
import android.content.SharedPreferences
import com.example.garmin.data.repository.BluetoothRepositoryImpl
import com.example.garmin.data.repository.SharedPreferenceStorageImpl
import com.example.garmin.data.source.GarminHRMDualManager
import com.example.garmin.domain.repository.BluetoothRepository
import com.example.garmin.domain.repository.SharedPreferenceStorage
import com.example.garmin.util.Constants.SESSION_PREF_NAME
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object CoroutineDispatcherModule {
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun providesGarminHRMDualManager(@ApplicationContext context: Context): GarminHRMDualManager = GarminHRMDualManager( context)


    @Provides
    @Named(SESSION_PREF_NAME)
    @Singleton
    internal fun provideSharedPreferencesForSessionCache(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(SESSION_PREF_NAME, Context.MODE_PRIVATE)
    }


    @Provides
    @Singleton
    fun provideSharedPreferenceStorageImpl(
        @Named(SESSION_PREF_NAME) sharedPreferences:SharedPreferences):SharedPreferenceStorage
    {
       return SharedPreferenceStorageImpl(sharedPreferences)
    }

}

@Module
@InstallIn(ViewModelComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindRemoteRepositoryImpl(remoteRepository: BluetoothRepositoryImpl): BluetoothRepository


//    @Binds
//    abstract fun bindSharedPreferenceStorageImpl(sharedPreferenceStorageImpl: SharedPreferenceStorageImpl): SharedPreferenceStorage


}