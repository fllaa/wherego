package com.flla.wherego.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WheregoDatabase =
        Room.databaseBuilder(context, WheregoDatabase::class.java, "wherego.db")
            .addMigrations(
                WheregoDatabase.MIGRATION_1_2,
                WheregoDatabase.MIGRATION_2_3,
                WheregoDatabase.MIGRATION_3_4,
                WheregoDatabase.MIGRATION_4_5,
                WheregoDatabase.MIGRATION_5_6,
                WheregoDatabase.MIGRATION_6_7,
                WheregoDatabase.MIGRATION_7_8,
                WheregoDatabase.MIGRATION_8_9,
            )
            .build()

    @Provides
    fun provideUserProfileDao(db: WheregoDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideCategoryDao(db: WheregoDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: WheregoDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideSyncStateDao(db: WheregoDatabase): SyncStateDao = db.syncStateDao()

    @Provides
    fun provideBudgetDao(db: WheregoDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideRecurringDao(db: WheregoDatabase): RecurringDao = db.recurringDao()

    @Provides
    fun provideReceiptDao(db: WheregoDatabase): ReceiptDao = db.receiptDao()

    @Provides
    fun provideGoalDao(db: WheregoDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideFxRateDao(db: WheregoDatabase): FxRateDao = db.fxRateDao()

    @Provides
    @Singleton
    fun provideLocalDataEraser(impl: RoomLocalDataEraser): LocalDataEraser = impl
}
