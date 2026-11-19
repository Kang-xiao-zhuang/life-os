package com.zk.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zk.lifeos.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

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

    /**
     * Archived projects, newest first. Without this, archiving was a one-way trip: every other
     * query filters `archived = 0`, so an archived project became unreachable — the opposite of
     * what "archive, don't delete" is supposed to mean.
     */
    @Query(
        """
        SELECT p.*,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.done = 0) AS openTasks,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.done = 1) AS doneTasks
        FROM projects p
        WHERE p.archived = 1
        ORDER BY p.updatedAt DESC
        """
    )
    fun observeArchivedWithCounts(): Flow<List<ProjectWithCounts>>

    @Query("SELECT COUNT(*) FROM projects WHERE archived = 1")
    fun observeArchivedCount(): Flow<Int>

    /**
     * Permanent delete, only offered for projects that are already archived. Its tasks survive:
     * the foreign key is `SET NULL`, so they fall back to 未归类 rather than disappearing.
     */
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Query("UPDATE projects SET name = :name, emoji = :emoji, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, emoji: String, now: Long)

    /**
     * Archive instead of delete: a project's tasks and history stay, it just leaves the lists.
     * Nothing the user recorded is destroyed by tidying up.
     */
    @Query("UPDATE projects SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long)

    /** Next free slot at the end of the manual ordering. */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM projects")
    suspend fun nextSortOrder(): Int

    // ---- backup / restore ----
    // Archived rows included: a backup must be everything, not just what the lists show.

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectEntity>

    @Insert
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}
