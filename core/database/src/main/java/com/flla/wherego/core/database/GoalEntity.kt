package com.flla.wherego.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flla.wherego.core.model.Goal

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val allocatedMinor: Long,
    val currency: String,
    val updatedAt: Long,
    val targetMinor: Long = 0L,
) {
    fun toModel(): Goal = Goal(id, name, allocatedMinor, currency, updatedAt, targetMinor)

    companion object {
        fun from(model: Goal): GoalEntity = GoalEntity(
            id = model.id,
            name = model.name,
            allocatedMinor = model.allocatedMinor,
            currency = model.currency,
            updatedAt = model.updatedAt,
            targetMinor = model.targetMinor,
        )
    }
}
