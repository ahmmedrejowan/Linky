package com.rejowan.linky.data.mapper

import com.rejowan.linky.data.local.database.entity.CollectionEntity
import com.rejowan.linky.data.local.database.entity.LinkEntity
import com.rejowan.linky.data.local.database.entity.SnapshotEntity
import com.rejowan.linky.data.mapper.CollectionMapper.toDomain
import com.rejowan.linky.data.mapper.CollectionMapper.toDomainList
import com.rejowan.linky.data.mapper.CollectionMapper.toEntity
import com.rejowan.linky.data.mapper.CollectionMapper.toEntityList
import com.rejowan.linky.data.mapper.LinkMapper.toDomain
import com.rejowan.linky.data.mapper.LinkMapper.toDomainList
import com.rejowan.linky.data.mapper.LinkMapper.toEntity
import com.rejowan.linky.data.mapper.LinkMapper.toEntityList
import com.rejowan.linky.data.mapper.SnapshotMapper.toDomain
import com.rejowan.linky.data.mapper.SnapshotMapper.toDomainList
import com.rejowan.linky.data.mapper.SnapshotMapper.toEntity
import com.rejowan.linky.data.mapper.SnapshotMapper.toEntityList
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.model.SnapshotType
import org.junit.Assert.*
import org.junit.Test

class MapperTest {

    // =================== Link Mapper Tests ===================

    @Test
    fun `LinkMapper toEntity maps all fields correctly`() {
        val link = Link(
            id = "link-1",
            title = "Test Link",
            description = "Test Description",
            url = "https://example.com",
            note = "Test Note",
            collectionId = "collection-1",
            previewImagePath = "/preview.png",
            previewUrl = "https://example.com/preview",
            isFavorite = true,
            isArchived = true,
            hideFromHome = true,
            deletedAt = 3000L,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = link.toEntity(syncToRemote = true, userId = "user-1")

        assertEquals(link.id, entity.id)
        assertEquals(link.title, entity.title)
        assertEquals(link.description, entity.description)
        assertEquals(link.url, entity.url)
        assertEquals(link.note, entity.note)
        assertEquals(link.collectionId, entity.collectionId)
        assertEquals(link.previewImagePath, entity.previewImagePath)
        assertEquals(link.previewUrl, entity.previewUrl)
        assertEquals(link.isFavorite, entity.isFavorite)
        assertEquals(link.isArchived, entity.isArchived)
        assertEquals(link.hideFromHome, entity.hideFromHome)
        assertEquals(link.deletedAt, entity.deletedAt)
        assertEquals(link.createdAt, entity.createdAt)
        assertEquals(link.updatedAt, entity.updatedAt)
        assertTrue(entity.syncToRemote)
        assertEquals("user-1", entity.userId)
    }

    @Test
    fun `LinkMapper toDomain maps all fields correctly`() {
        val entity = LinkEntity(
            id = "link-1",
            title = "Test Link",
            description = "Test Description",
            url = "https://example.com",
            note = "Test Note",
            collectionId = "collection-1",
            previewImagePath = "/preview.png",
            previewUrl = "https://example.com/preview",
            isFavorite = true,
            isArchived = true,
            hideFromHome = true,
            deletedAt = 3000L,
            createdAt = 1000L,
            updatedAt = 2000L,
            syncToRemote = true,
            userId = "user-1"
        )

        val link = entity.toDomain()

        assertEquals(entity.id, link.id)
        assertEquals(entity.title, link.title)
        assertEquals(entity.description, link.description)
        assertEquals(entity.url, link.url)
        assertEquals(entity.note, link.note)
        assertEquals(entity.collectionId, link.collectionId)
        assertEquals(entity.previewImagePath, link.previewImagePath)
        assertEquals(entity.previewUrl, link.previewUrl)
        assertEquals(entity.isFavorite, link.isFavorite)
        assertEquals(entity.isArchived, link.isArchived)
        assertEquals(entity.hideFromHome, link.hideFromHome)
        assertEquals(entity.deletedAt, link.deletedAt)
        assertEquals(entity.createdAt, link.createdAt)
        assertEquals(entity.updatedAt, link.updatedAt)
    }

    @Test
    fun `LinkMapper handles null fields correctly`() {
        val link = Link(
            id = "link-1",
            title = "Test",
            url = "https://example.com",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = link.toEntity()

        assertNull(entity.description)
        assertNull(entity.note)
        assertNull(entity.collectionId)
        assertNull(entity.previewImagePath)
        assertNull(entity.previewUrl)
        assertNull(entity.deletedAt)
    }

    @Test
    fun `LinkMapper list conversion works correctly`() {
        val links = listOf(
            Link(id = "1", title = "Link 1", url = "https://1.com", createdAt = 1000L, updatedAt = 1000L),
            Link(id = "2", title = "Link 2", url = "https://2.com", createdAt = 2000L, updatedAt = 2000L)
        )

        val entities = links.toEntityList()
        assertEquals(2, entities.size)
        assertEquals("1", entities[0].id)
        assertEquals("2", entities[1].id)

        val backToLinks = entities.toDomainList()
        assertEquals(2, backToLinks.size)
        assertEquals("Link 1", backToLinks[0].title)
        assertEquals("Link 2", backToLinks[1].title)
    }

    // =================== Collection Mapper Tests ===================

    @Test
    fun `CollectionMapper toEntity maps all fields correctly`() {
        val collection = Collection(
            id = "collection-1",
            name = "Work",
            color = "#FF5733",
            icon = "work",
            isFavorite = true,
            sortOrder = 5,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = collection.toEntity(syncToRemote = true, userId = "user-1")

        assertEquals(collection.id, entity.id)
        assertEquals(collection.name, entity.name)
        assertEquals(collection.color, entity.color)
        assertEquals(collection.icon, entity.icon)
        assertEquals(collection.isFavorite, entity.isFavorite)
        assertEquals(collection.sortOrder, entity.sortOrder)
        assertEquals(collection.createdAt, entity.createdAt)
        assertEquals(collection.updatedAt, entity.updatedAt)
        assertTrue(entity.syncToRemote)
        assertEquals("user-1", entity.userId)
    }

    @Test
    fun `CollectionMapper toDomain maps all fields correctly`() {
        val entity = CollectionEntity(
            id = "collection-1",
            name = "Work",
            color = "#FF5733",
            icon = "work",
            isFavorite = true,
            sortOrder = 5,
            createdAt = 1000L,
            updatedAt = 2000L,
            syncToRemote = true,
            userId = "user-1"
        )

        val collection = entity.toDomain()

        assertEquals(entity.id, collection.id)
        assertEquals(entity.name, collection.name)
        assertEquals(entity.color, collection.color)
        assertEquals(entity.icon, collection.icon)
        assertEquals(entity.isFavorite, collection.isFavorite)
        assertEquals(entity.sortOrder, collection.sortOrder)
        assertEquals(entity.createdAt, collection.createdAt)
        assertEquals(entity.updatedAt, collection.updatedAt)
    }

    @Test
    fun `CollectionMapper handles null fields correctly`() {
        val collection = Collection(
            id = "collection-1",
            name = "Work",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = collection.toEntity()

        assertNull(entity.color)
        assertNull(entity.icon)
    }

    @Test
    fun `CollectionMapper list conversion works correctly`() {
        val collections = listOf(
            Collection(id = "1", name = "Work", createdAt = 1000L, updatedAt = 1000L),
            Collection(id = "2", name = "Personal", createdAt = 2000L, updatedAt = 2000L)
        )

        val entities = collections.toEntityList()
        assertEquals(2, entities.size)

        val backToCollections = entities.toDomainList()
        assertEquals(2, backToCollections.size)
        assertEquals("Work", backToCollections[0].name)
        assertEquals("Personal", backToCollections[1].name)
    }

    // =================== Snapshot Mapper Tests ===================

    @Test
    fun `SnapshotMapper toEntity maps all fields correctly`() {
        val snapshot = Snapshot(
            id = "snapshot-1",
            linkId = "link-1",
            type = SnapshotType.READER_MODE,
            filePath = "/path/to/file.html",
            fileSize = 1024L,
            createdAt = 1000L,
            title = "Article Title",
            author = "Author Name",
            excerpt = "Article excerpt",
            wordCount = 500,
            estimatedReadTime = 3
        )

        val entity = snapshot.toEntity()

        assertEquals(snapshot.id, entity.id)
        assertEquals(snapshot.linkId, entity.linkId)
        assertEquals("READER_MODE", entity.type)
        assertEquals(snapshot.filePath, entity.filePath)
        assertEquals(snapshot.fileSize, entity.fileSize)
        assertEquals(snapshot.createdAt, entity.createdAt)
        assertEquals(snapshot.title, entity.title)
        assertEquals(snapshot.author, entity.author)
        assertEquals(snapshot.excerpt, entity.excerpt)
        assertEquals(snapshot.wordCount, entity.wordCount)
        assertEquals(snapshot.estimatedReadTime, entity.estimatedReadTime)
    }

    @Test
    fun `SnapshotMapper toDomain maps all fields correctly`() {
        val entity = SnapshotEntity(
            id = "snapshot-1",
            linkId = "link-1",
            type = "READER_MODE",
            filePath = "/path/to/file.html",
            fileSize = 1024L,
            createdAt = 1000L,
            title = "Article Title",
            author = "Author Name",
            excerpt = "Article excerpt",
            wordCount = 500,
            estimatedReadTime = 3
        )

        val snapshot = entity.toDomain()

        assertEquals(entity.id, snapshot.id)
        assertEquals(entity.linkId, snapshot.linkId)
        assertEquals(SnapshotType.READER_MODE, snapshot.type)
        assertEquals(entity.filePath, snapshot.filePath)
        assertEquals(entity.fileSize, snapshot.fileSize)
        assertEquals(entity.createdAt, snapshot.createdAt)
        assertEquals(entity.title, snapshot.title)
        assertEquals(entity.author, snapshot.author)
        assertEquals(entity.excerpt, snapshot.excerpt)
        assertEquals(entity.wordCount, snapshot.wordCount)
        assertEquals(entity.estimatedReadTime, snapshot.estimatedReadTime)
    }

    @Test
    fun `SnapshotMapper handles different snapshot types`() {
        val pdfSnapshot = Snapshot(
            id = "snapshot-2",
            linkId = "link-1",
            type = SnapshotType.PDF,
            filePath = "/path/to/file.pdf",
            fileSize = 2048L,
            createdAt = 1000L
        )

        val pdfEntity = pdfSnapshot.toEntity()
        assertEquals("PDF", pdfEntity.type)

        val screenshotSnapshot = Snapshot(
            id = "snapshot-3",
            linkId = "link-1",
            type = SnapshotType.SCREENSHOT,
            filePath = "/path/to/file.png",
            fileSize = 4096L,
            createdAt = 1000L
        )

        val screenshotEntity = screenshotSnapshot.toEntity()
        assertEquals("SCREENSHOT", screenshotEntity.type)
    }

    @Test
    fun `SnapshotMapper handles null fields correctly`() {
        val snapshot = Snapshot(
            id = "snapshot-1",
            linkId = "link-1",
            type = SnapshotType.PDF,
            filePath = "/path/to/file.pdf",
            fileSize = 1024L,
            createdAt = 1000L
        )

        val entity = snapshot.toEntity()

        assertNull(entity.title)
        assertNull(entity.author)
        assertNull(entity.excerpt)
        assertNull(entity.wordCount)
        assertNull(entity.estimatedReadTime)
    }

    @Test
    fun `SnapshotMapper list conversion works correctly`() {
        val snapshots = listOf(
            Snapshot("1", "link-1", SnapshotType.READER_MODE, "/path1.html", 1024L, 1000L),
            Snapshot("2", "link-2", SnapshotType.PDF, "/path2.pdf", 2048L, 2000L)
        )

        val entities = snapshots.toEntityList()
        assertEquals(2, entities.size)

        val backToSnapshots = entities.toDomainList()
        assertEquals(2, backToSnapshots.size)
        assertEquals(SnapshotType.READER_MODE, backToSnapshots[0].type)
        assertEquals(SnapshotType.PDF, backToSnapshots[1].type)
    }

    // =================== VaultLinkMapper Tests ===================

    @Test
    fun `VaultLinkMapper fromLink converts regular link to vault link`() {
        val link = Link(
            id = "link-1",
            title = "Secret Link",
            description = "Secret Description",
            url = "https://secret.example.com",
            note = "Secret Notes",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val vaultLink = VaultLinkMapper.fromLink(link)

        assertEquals(link.url, vaultLink.url)
        assertEquals(link.title, vaultLink.title)
        assertEquals(link.description, vaultLink.description)
        assertEquals(link.note, vaultLink.notes)
    }

    @Test
    fun `VaultLinkMapper fromLink handles null fields correctly`() {
        val link = Link(
            id = "link-1",
            title = "Simple Link",
            url = "https://example.com",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val vaultLink = VaultLinkMapper.fromLink(link)

        assertEquals(link.url, vaultLink.url)
        assertEquals(link.title, vaultLink.title)
        assertNull(vaultLink.description)
        assertNull(vaultLink.notes)
    }
}
