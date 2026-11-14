package com.huzeifa.cubomagico.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "solve_times")
data class SolveTime(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMillis: Long,
    val scramble: String,
    val createdAt: Long,
    val penalty: String = "OK"
)

@Dao
interface SolveTimeDao {
    @Insert suspend fun insert(entry: SolveTime): Long
    @Query("SELECT * FROM solve_times ORDER BY createdAt DESC LIMIT 100")
    suspend fun latest(): List<SolveTime>
    @Query("UPDATE solve_times SET penalty = :penalty WHERE id = :id")
    suspend fun updatePenalty(id: Long, penalty: String)
    @Query("DELETE FROM solve_times")
    suspend fun deleteAll()
}

@Database(entities = [SolveTime::class], version = 1, exportSchema = false)
abstract class CubeDatabase : RoomDatabase() {
    abstract fun solveTimeDao(): SolveTimeDao
}
