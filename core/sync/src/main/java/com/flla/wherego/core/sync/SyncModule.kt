package com.flla.wherego.core.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    @Singleton
    abstract fun auth(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun cloud(impl: FirestoreCloudDataSource): CloudDataSource

    @Binds
    @Singleton
    abstract fun receipts(impl: FirebaseReceiptUploader): ReceiptUploader
}
