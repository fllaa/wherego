package app.wherego.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rows: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE deletedAt IS NULL AND archived = 0 ORDER BY sortOrder ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE deletedAt IS NULL AND archived = 0 ORDER BY sortOrder ASC")
    suspend fun listActive(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CategoryEntity?
}
