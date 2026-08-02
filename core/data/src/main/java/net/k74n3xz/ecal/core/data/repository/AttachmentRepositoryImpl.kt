package net.k74n3xz.ecal.core.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import net.k74n3xz.ecal.core.application.port.out.repository.AttachmentRepository
import net.k74n3xz.ecal.core.data.database.dao.AttachmentDao
import net.k74n3xz.ecal.core.data.database.entity.AttachmentEntity
import net.k74n3xz.ecal.core.data.database.entity.enumeration.attachment.FileState
import net.k74n3xz.ecal.core.data.utils.toAttachment
import net.k74n3xz.ecal.core.model.Attachment

internal class AttachmentRepositoryImpl @Inject constructor(
    private val attachmentDao: AttachmentDao,
    @param:ApplicationContext private val context: Context
) : AttachmentRepository {
    private companion object {
        private const val ATTACHMENT_DIR_NAME: String = "Attachments"
    }

    // TODO: Provide a fallback when getExternalFilesDir() returns null.
    private val rootDir = context.applicationContext.getExternalFilesDir(null)!!
    private val attachmentDir = File(rootDir, ATTACHMENT_DIR_NAME)

    private val contentResolver: ContentResolver = context.applicationContext.contentResolver

    override suspend fun findAttachmentById(id: Long): Attachment? = attachmentDao.queryById(id)?.toAttachment()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAvailableAttachments(): Flow<List<Attachment>> =
        attachmentDao.observeAttachmentsByFileState(FileState.OK).mapLatest { list -> list.map { it.toAttachment() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAvailableAttachmentsByMimeTopLevelType(topLevelType: String): Flow<List<Attachment>> =
        attachmentDao.observeAttachmentsByFileStateAndMimeTopLevelType(FileState.OK, topLevelType)
            .mapLatest { list -> list.map { it.toAttachment() } }

    override suspend fun addAttachment(description: String?, platformToken: String) {
        val id: Long
        val uri = platformToken.toUri()
        val destination = File(attachmentDir, Uuid.random().toHexDashString())

        require(
            uri.scheme != ContentResolver.SCHEME_CONTENT || (
                DocumentsContract.isDocumentUri(context.applicationContext, uri) &&
                    contentResolver.getType(uri) != DocumentsContract.Document.MIME_TYPE_DIR
                )
        ) {
            "Expected a non-directory document URI from a DocumentsProvider."
        }

        contentResolver.query(
            uri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ),
            null,
            null,
            null
        )?.use {
            val nameIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeTypeIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeBytesIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

            if (it.count == 1 && it.moveToFirst()) {
                id = attachmentDao.queryIdByRowId(
                    attachmentDao.insert(
                        AttachmentEntity(
                            id = null,
                            description = description,
                            name = it.getString(nameIndex),
                            mimeType = it.getString(mimeTypeIndex),
                            sizeBytes = it.getLong(sizeBytesIndex),
                            relativePath = destination.relativeTo(rootDir).path,
                            platformToken = uri.toString(),
                            state = FileState.IMPORTING
                        )
                    ).single()
                )!!
            } else {
                throw IOException("The file metadata query returned an invalid result.")
            }
        } ?: throw IOException("Failed to retrieve file metadata.")

        try {
            if (!attachmentDir.exists() && !attachmentDir.mkdir()) {
                throw IOException("Failed to create attachment directory.")
            }

            contentResolver.openInputStream(uri)?.use { inputStream ->
                destination.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("Failed to copy the file.")
        } catch (exception: Exception) {
            attachmentDao.updateFileStateById(id, FileState.IMPORT_FAILED)
            throw exception
        }

        attachmentDao.updateFileStateById(id, FileState.OK)
    }

    override suspend fun updateAttachmentDescription(id: Long, description: String?) {
        attachmentDao.updateDescriptionById(id, description)
    }

    override suspend fun deleteAttachmentById(id: Long) {
        attachmentDao.updateFileStateById(id, FileState.REMOVING)
    }

    override suspend fun cleanup() {
        attachmentDao.queryByFileState(FileState.IMPORT_FAILED, FileState.REMOVING).forEach {
            val file = File(rootDir, it.relativePath)
            if (!file.exists() || file.delete()) {
                attachmentDao.deleteById(it.id!!)
            }
        }
    }

    override suspend fun openAttachmentById(id: Long): FileInputStream? =
        attachmentDao.queryById(id)?.let { File(rootDir, it.relativePath).inputStream() }

    override suspend fun generateAttachmentSharingTokenById(id: Long): String? = attachmentDao.queryById(id)?.let {
        FileProvider.getUriForFile(
            /* context = */
            context.applicationContext,
            /* authority = */
            "${context.packageName}.fileprovider",
            /* file = */
            File(rootDir, it.relativePath)
        ).toString()
    }
}
