package com.rejowan.linky.data.mapper

import com.rejowan.linky.data.local.database.entity.VaultLinkEntity
import com.rejowan.linky.data.security.EncryptedData
import com.rejowan.linky.data.security.VaultSessionManager
import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.model.VaultLinkData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Mapper for VaultLink domain model to/from VaultLinkEntity.
 * Handles encryption/decryption of link data.
 */
object VaultLinkMapper {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Convert VaultLink domain model to encrypted VaultLinkEntity
     * @param vaultLink The decrypted vault link
     * @param sessionManager Session manager for encryption
     * @return Encrypted entity or null if encryption fails
     */
    fun toEntity(vaultLink: VaultLink, sessionManager: VaultSessionManager): VaultLinkEntity? {
        val data = VaultLinkData(
            url = vaultLink.url,
            title = vaultLink.title,
            description = vaultLink.description,
            notes = vaultLink.notes
        )

        val jsonString = json.encodeToString(data)
        val encrypted = sessionManager.encrypt(jsonString) ?: return null

        return VaultLinkEntity(
            id = vaultLink.id,
            encryptedData = encrypted.ciphertext,
            iv = encrypted.iv,
            createdAt = vaultLink.createdAt,
            updatedAt = vaultLink.updatedAt
        )
    }

    /**
     * Convert encrypted VaultLinkEntity to VaultLink domain model
     * @param entity The encrypted entity
     * @param sessionManager Session manager for decryption
     * @return Decrypted vault link or null if decryption fails
     */
    fun toDomain(entity: VaultLinkEntity, sessionManager: VaultSessionManager): VaultLink? {
        val encrypted = EncryptedData(
            ciphertext = entity.encryptedData,
            iv = entity.iv
        )

        val jsonString = sessionManager.decrypt(encrypted) ?: return null

        return try {
            val data = json.decodeFromString<VaultLinkData>(jsonString)
            VaultLink(
                id = entity.id,
                url = data.url,
                title = data.title,
                description = data.description,
                notes = data.notes,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert a regular Link to a VaultLink
     */
    fun fromLink(link: com.rejowan.linky.domain.model.Link): VaultLink {
        return VaultLink(
            url = link.url,
            title = link.title,
            description = link.description,
            notes = link.note,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
