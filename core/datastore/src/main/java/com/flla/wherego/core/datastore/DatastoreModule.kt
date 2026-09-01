package com.flla.wherego.core.datastore

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatastoreModule {
    @Binds
    @Singleton
    abstract fun pinMac(impl: KeystorePinMac): PinMac
}
