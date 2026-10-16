package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ExtractionPreset
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val videoUriString: String,
    val durationMs: Long,
    val frameCount: Int,
    val favoriteCount: Int,
    val createdAt: Long,
    val previewThumbPath: String?
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteById(projectId: String)

    @Query("DELETE FROM projects")
    suspend fun clearAll()
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "photo_frame_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object PresetRepository {
    val defaultPresets = listOf(
        ExtractionPreset(
            id = "preset_sports",
            title = "High-Speed Action (16.7ms)",
            description = "Ideal for 60 FPS sports, athletics, and dynamic motion",
            intervalUs = 16_667L,
            intervalLabel = "16.7 ms (~60 FPS)"
        ),
        ExtractionPreset(
            id = "preset_cinema",
            title = "Cinematic Real-Time (33.3ms)",
            description = "Standard 30 FPS video playback extraction",
            intervalUs = 33_333L,
            intervalLabel = "33.3 ms (~30 FPS)"
        ),
        ExtractionPreset(
            id = "preset_slowmo",
            title = "Ultra Slow-Mo Burst (5ms)",
            description = "Deep microsecond precision inspection for fast movements",
            intervalUs = 5_000L,
            intervalLabel = "5 ms (200 Hz)"
        ),
        ExtractionPreset(
            id = "preset_1ms",
            title = "Millisecond Precision (1ms)",
            description = "Finest 1 millisecond timestamp grid for exact moments",
            intervalUs = 1_000L,
            intervalLabel = "1.0 ms"
        ),
        ExtractionPreset(
            id = "preset_dance",
            title = "Dance & Expression (50ms)",
            description = "20 FPS pace for capturing smiles, poses, and choreography",
            intervalUs = 50_000L,
            intervalLabel = "50 ms (20 FPS)"
        ),
        ExtractionPreset(
            id = "preset_burst",
            title = "Camera Shutter Burst (100ms)",
            description = "Simulates 10 FPS continuous DSLR shutter bursts",
            intervalUs = 100_000L,
            intervalLabel = "100 ms (10 FPS)"
        ),
        ExtractionPreset(
            id = "preset_action",
            title = "Motion Checkpoints (250ms)",
            description = "4 Frames per second for event scanning",
            intervalUs = 250_000L,
            intervalLabel = "250 ms (4 FPS)"
        ),
        ExtractionPreset(
            id = "preset_vlog",
            title = "Vlog Highlights (1s)",
            description = "1 Frame every second for travel logs and landscape cuts",
            intervalUs = 1_000_000L,
            intervalLabel = "1.0 second"
        ),
        ExtractionPreset(
            id = "preset_timelapse",
            title = "Time-Lapse Overview (5s)",
            description = "1 Frame every 5 seconds for long duration summaries",
            intervalUs = 5_000_000L,
            intervalLabel = "5.0 seconds"
        )
    )
}
