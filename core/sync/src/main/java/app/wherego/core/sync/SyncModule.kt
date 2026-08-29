package app.wherego.core.sync

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
    abstract fun auth(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun cloud(impl: FakeCloudDataSource): CloudDataSource
}
