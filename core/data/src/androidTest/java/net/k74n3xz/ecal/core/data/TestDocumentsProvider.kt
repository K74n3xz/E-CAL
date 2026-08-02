package net.k74n3xz.ecal.core.data

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException

private data class DocumentRecord(
    val name: String,
    val mimeType: String,
    val content: ByteArray,
    val metadataRows: Int,
    val failOnOpen: Boolean
)

class TestDocumentsProvider : DocumentsProvider() {
    companion object {
        private const val ROOT_ID = "root"
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_FLAGS
        )
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_FLAGS
        )
        private val records = mutableMapOf<String, DocumentRecord>()

        fun register(
            context: Context,
            id: String,
            name: String = "$id.txt",
            mimeType: String = "text/plain",
            content: ByteArray = id.encodeToByteArray(),
            metadataRows: Int = 1,
            failOnOpen: Boolean = false
        ): Uri {
            records[id] = DocumentRecord(name, mimeType, content, metadataRows, failOnOpen)
            return DocumentsContract.buildDocumentUri("${context.packageName}.documents", id)
        }

        fun clear() = records.clear()
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val columns = projection ?: DEFAULT_ROOT_PROJECTION
        return MatrixCursor(columns).apply {
            addRow(Array(columns.size) { index -> rootColumnValue(columns[index]) })
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val columns = projection ?: DEFAULT_DOCUMENT_PROJECTION
        val record = records[documentId] ?: throw FileNotFoundException(documentId)
        return MatrixCursor(columns).apply {
            repeat(record.metadataRows) {
                addRow(Array(columns.size) { index -> documentColumnValue(columns[index], documentId, record) })
            }
        }
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val record = records[documentId] ?: throw FileNotFoundException(documentId)
        if (record.failOnOpen) throw FileNotFoundException("Configured failure for $documentId")
        val file = File(requireNotNull(context).cacheDir, "documents-provider-$documentId")
        file.writeBytes(record.content)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun rootColumnValue(column: String): Any? = when (column) {
        DocumentsContract.Root.COLUMN_ROOT_ID -> ROOT_ID
        DocumentsContract.Root.COLUMN_DOCUMENT_ID -> ROOT_ID
        DocumentsContract.Root.COLUMN_TITLE -> "ECAL test documents"
        DocumentsContract.Root.COLUMN_FLAGS -> 0
        else -> null
    }

    private fun documentColumnValue(column: String, documentId: String, record: DocumentRecord): Any? = when (column) {
        DocumentsContract.Document.COLUMN_DOCUMENT_ID -> documentId
        DocumentsContract.Document.COLUMN_DISPLAY_NAME -> record.name
        DocumentsContract.Document.COLUMN_MIME_TYPE -> record.mimeType
        DocumentsContract.Document.COLUMN_SIZE -> record.content.size.toLong()
        DocumentsContract.Document.COLUMN_FLAGS -> 0
        else -> null
    }
}
