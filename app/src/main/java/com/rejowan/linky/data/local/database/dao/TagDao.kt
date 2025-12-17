package com.rejowan.linky.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rejowan.linky.data.local.database.entity.LinkTagCrossRef
import com.rejowan.linky.data.local.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    // ============ TAG CRUD ============

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsOnce(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :id")
    fun getByIdFlow(id: String): Flow<TagEntity?>

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteById(tagId: String)

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun countTags(): Int

    @Query("SELECT COUNT(*) FROM tags")
    fun countTagsFlow(): Flow<Int>

    // ============ LINK-TAG ASSOCIATIONS ============

    /**
     * Get all tags for a specific link
     */
    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN link_tags ON tags.id = link_tags.tagId
        WHERE link_tags.linkId = :linkId
        ORDER BY tags.name ASC
    """)
    fun getTagsForLink(linkId: String): Flow<List<TagEntity>>

    /**
     * Get all tags for a specific link (one-time)
     */
    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN link_tags ON tags.id = link_tags.tagId
        WHERE link_tags.linkId = :linkId
        ORDER BY tags.name ASC
    """)
    suspend fun getTagsForLinkOnce(linkId: String): List<TagEntity>

    /**
     * Get all links that have a specific tag
     */
    @Query("""
        SELECT linkId FROM link_tags
        WHERE tagId = :tagId
    """)
    fun getLinkIdsForTag(tagId: String): Flow<List<String>>

    /**
     * Get count of links using each tag
     */
    @Query("""
        SELECT tags.*, COUNT(link_tags.linkId) as linkCount
        FROM tags
        LEFT JOIN link_tags ON tags.id = link_tags.tagId
        GROUP BY tags.id
        ORDER BY tags.name ASC
    """)
    fun getTagsWithLinkCount(): Flow<List<TagWithLinkCount>>

    /**
     * Add a tag to a link
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToLink(crossRef: LinkTagCrossRef)

    /**
     * Add multiple tags to a link
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagsToLink(crossRefs: List<LinkTagCrossRef>)

    /**
     * Remove a tag from a link
     */
    @Query("DELETE FROM link_tags WHERE linkId = :linkId AND tagId = :tagId")
    suspend fun removeTagFromLink(linkId: String, tagId: String)

    /**
     * Remove all tags from a link
     */
    @Query("DELETE FROM link_tags WHERE linkId = :linkId")
    suspend fun removeAllTagsFromLink(linkId: String)

    /**
     * Update tags for a link (replace all)
     */
    @Transaction
    suspend fun setTagsForLink(linkId: String, tagIds: List<String>) {
        removeAllTagsFromLink(linkId)
        if (tagIds.isNotEmpty()) {
            val crossRefs = tagIds.map { tagId -> LinkTagCrossRef(linkId, tagId) }
            addTagsToLink(crossRefs)
        }
    }

    /**
     * Check if a link has a specific tag
     */
    @Query("SELECT EXISTS(SELECT 1 FROM link_tags WHERE linkId = :linkId AND tagId = :tagId)")
    suspend fun linkHasTag(linkId: String, tagId: String): Boolean
}

/**
 * Data class for tag with associated link count
 */
data class TagWithLinkCount(
    val id: String,
    val name: String,
    val color: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val linkCount: Int
)
