package app.wherego.di

import app.wherego.core.common.UlidGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {
    @Provides
    @Singleton
    fun provideUlidGenerator(): UlidGenerator = UlidGenerator()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
