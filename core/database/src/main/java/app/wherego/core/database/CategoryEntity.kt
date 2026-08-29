package app.wherego.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.wherego.core.model.Category
import app.wherego.core.model.PresetCategories

@Entity(
    tableName = "categories",
    indices = [Index("archived")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val kind: String,
    val isPreset: Boolean,
    val archived: Boolean,
    val sortOrder: Int,
    val updatedAt: Long,
    val deletedAt: Long?,
) {
    fun toModel(): Category = Category(
        id = id,
        name = name,
        emoji = emoji,
        colorHex = colorHex,
        softColorHex = PresetCategories.softHex(id),
        kind = kind,
        isPreset = isPreset,
        archived = archived,
        sortOrder = sortOrder,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    companion object {
        fun from(model: Category): CategoryEntity = CategoryEntity(
            id = model.id,
            name = model.name,
            emoji = model.emoji,
            colorHex = model.colorHex,
            kind = model.kind,
            isPreset = model.isPreset,
            archived = model.archived,
            sortOrder = model.sortOrder,
            updatedAt = model.updatedAt,
            deletedAt = model.deletedAt,
        )
    }
}
