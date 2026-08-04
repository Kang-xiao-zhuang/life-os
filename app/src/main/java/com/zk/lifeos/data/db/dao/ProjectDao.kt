package com.zk.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zk.lifeos.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read-side only so far. Create / edit / delete land in Phase 3 (项目管理).
 */
@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects WHERE archived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT COUNT(*) FROM projects WHERE archived = 0")
    fun observeActiveCount(): Flow<Int>

    /** Counting in SQL rather than loading every task just to size a badge. */
    @Query(
        """
        SELECT p.*,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.done = 0) AS openTasks,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.done = 1) AS doneTasks
        FROM projects p
        WHERE p.archived = 0
        ORDER BY p.sortOrder ASC, p.id ASC
        """
    )
    fun observeActiveWithCounts(): Flow<List<ProjectWithCounts>>

    @Insert
    suspend fun insert(project: ProjectEntity): Long
}
