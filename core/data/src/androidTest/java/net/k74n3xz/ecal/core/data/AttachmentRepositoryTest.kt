package net.k74n3xz.ecal.core.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.data.database.AppDatabase
import net.k74n3xz.ecal.core.data.database.entity.enumeration.attachment.FileState
import net.k74n3xz.ecal.core.data.repository.AttachmentRepositoryImpl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttachmentRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: AttachmentRepositoryImpl

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        attachmentDirectory(context).deleteRecursively()
        TestDocumentsProvider.clear()
        database = createInMemoryDatabase()
        repository = AttachmentRepositoryImpl(database.attachmentDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
        attachmentDirectory(context).deleteRecursively()
        TestDocumentsProvider.clear()
    }

    @Test
    fun addAttachment_importsMetadataAndBytesAndMarksRowAvailable() = runTest {
        val content = "attachment content".encodeToByteArray()
        val uri = TestDocumentsProvider.register(
            context,
            id = "success",
            name = "agenda.txt",
            content = content
        )

        repository.addAttachment("Agenda", uri.toString())

        val entity = database.attachmentDao().queryByFileState(FileState.OK).single()
        assertEquals("Agenda", entity.description)
        assertEquals("agenda.txt", entity.name)
        assertEquals("text/plain", entity.mimeType)
        assertEquals(content.size.toLong(), entity.sizeBytes)
        assertEquals(uri.toString(), entity.platformToken)
        assertTrue(attachmentRootDirectory(context).resolve(entity.relativePath).readBytes().contentEquals(content))
        assertEquals(entity.id, repository.findAttachmentById(requireNotNull(entity.id))?.id)
        repository.openAttachmentById(requireNotNull(entity.id))!!.use { stream ->
            assertTrue(stream.readBytes().contentEquals(content))
        }
    }

    @Test
    fun addAttachment_rejectsDirectoryDocument() = runTest {
        val uri = TestDocumentsProvider.register(
            context,
            id = "directory",
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR
        )

        val error = assertSuspendFails<IllegalArgumentException> {
            repository.addAttachment(null, uri.toString())
        }

        assertEquals("Expected a non-directory document URI from a DocumentsProvider.", error.message)
        assertTrue(allAttachmentRows().isEmpty())
    }

    @Test
    fun addAttachment_rejectsInvalidMetadataAndMarksCopyFailure() = runTest {
        val invalidMetadata = TestDocumentsProvider.register(
            context,
            id = "invalid-metadata",
            metadataRows = 0
        )
        assertSuspendFails<IOException> {
            repository.addAttachment(null, invalidMetadata.toString())
        }
        assertTrue(allAttachmentRows().isEmpty())

        val copyFailure = TestDocumentsProvider.register(
            context,
            id = "copy-failure",
            failOnOpen = true
        )
        assertSuspendFails<IOException> {
            repository.addAttachment(null, copyFailure.toString())
        }

        assertEquals(FileState.IMPORT_FAILED, allAttachmentRows().single().state)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAvailableAttachments_emitsOnlyOkRows() = runTest {
        database.insertAttachment(id = 1, state = FileState.OK)
        database.insertAttachment(id = 2, state = FileState.IMPORTING)
        val emissions = mutableListOf<Set<Long?>>()
        val firstEmission = CompletableDeferred<Unit>()
        val secondEmission = CompletableDeferred<Unit>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeAvailableAttachments().take(2).collect { attachments ->
                emissions += attachments.map { it.id }.toSet()
                if (emissions.size == 1) firstEmission.complete(Unit) else secondEmission.complete(Unit)
            }
        }

        firstEmission.await()
        database.attachmentDao().updateFileStateById(2, FileState.OK)
        secondEmission.await()
        collection.join()

        assertEquals(
            listOf(setOf(1L), setOf(1L, 2L)),
            emissions
        )
    }

    @Test
    fun observeAvailableAudioAttachments_filtersByStateAndMimeTopLevelType() = runTest {
        database.insertAttachment(id = 1, mimeType = "audio/mpeg")
        database.insertAttachment(id = 2, mimeType = "audio/wav", state = FileState.IMPORTING)
        database.insertAttachment(id = 3, mimeType = "application/ogg")

        val attachments = repository.observeAvailableAttachmentsByMimeTopLevelType("audio").take(1).toList().single()

        assertEquals(listOf(1L), attachments.map { it.id })
    }

    @Test
    fun updateDeleteAndCleanup_keepFileAndDatabaseStateConsistent() = runTest {
        val path = "Attachments/cleanup-file"
        database.insertAttachment(id = 1, description = "Old", relativePath = path)
        val file = attachmentRootDirectory(context).resolve(path)
        file.parentFile!!.mkdirs()
        file.writeText("content")

        repository.updateAttachmentDescription(1, "Updated")
        assertEquals("Updated", repository.findAttachmentById(1)?.description)

        repository.deleteAttachmentById(1)
        assertEquals(FileState.REMOVING, database.attachmentDao().queryById(1)?.state)
        repository.cleanup()

        assertNull(database.attachmentDao().queryById(1))
        assertTrue(!file.exists())
    }

    @Test
    fun cleanup_removesDatabaseRowWhenFileIsAlreadyMissing() = runTest {
        database.insertAttachment(id = 1, state = FileState.REMOVING, relativePath = "Attachments/missing")

        repository.cleanup()

        assertNull(database.attachmentDao().queryById(1))
    }

    @Test
    fun cleanup_removesImportFailedAndRemovingRowsAndFiles() = runTest {
        val directory = attachmentDirectory(context).apply { mkdirs() }
        val importFailedFile = directory.resolve("import-failed").apply { writeText("partial") }
        val removingFile = directory.resolve("removing").apply { writeText("content") }
        database.insertAttachment(
            id = 1,
            state = FileState.IMPORT_FAILED,
            relativePath = "Attachments/${importFailedFile.name}"
        )
        database.insertAttachment(
            id = 2,
            state = FileState.REMOVING,
            relativePath = "Attachments/${removingFile.name}"
        )

        assertEquals(
            setOf(1L, 2L),
            database.attachmentDao()
                .queryByFileState(FileState.IMPORT_FAILED, FileState.REMOVING)
                .map { it.id }
                .toSet()
        )

        repository.cleanup()

        assertNull(database.attachmentDao().queryById(1))
        assertNull(database.attachmentDao().queryById(2))
        assertTrue(!importFailedFile.exists())
        assertTrue(!removingFile.exists())
    }

    @Test
    fun generateSharingToken_returnsReadableFileProviderUri() = runTest {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val targetAttachmentDirectory = attachmentDirectory(targetContext)
        targetAttachmentDirectory.deleteRecursively()
        val content = "shared attachment".encodeToByteArray()
        val file = targetAttachmentDirectory.resolve("shared.txt").apply {
            parentFile!!.mkdirs()
            writeBytes(content)
        }
        database.insertAttachment(id = 7, relativePath = "Attachments/${file.name}")
        val targetRepository = AttachmentRepositoryImpl(database.attachmentDao(), targetContext)

        try {
            val uri = Uri.parse(requireNotNull(targetRepository.generateAttachmentSharingTokenById(7)))

            assertEquals("content", uri.scheme)
            assertEquals("${targetContext.packageName}.fileprovider", uri.authority)
            assertTrue(uri.path.orEmpty().startsWith("/attachments/"))
            targetContext.contentResolver.openInputStream(uri)!!.use { stream ->
                assertTrue(stream.readBytes().contentEquals(content))
            }
        } finally {
            targetAttachmentDirectory.deleteRecursively()
        }
    }

    private suspend fun allAttachmentRows() = FileState.entries.flatMap { state ->
        database.attachmentDao().queryByFileState(state)
    }

    private suspend inline fun <reified T : Throwable> assertSuspendFails(crossinline block: suspend () -> Unit): T {
        val exception = runCatching { block() }.exceptionOrNull()
        assertTrue("Expected ${T::class.java.name}, got $exception", exception is T)
        return exception as T
    }
}
