package com.example.data.files

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.local.ProjectFileEntity
import com.example.data.local.StudioFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class SafDocumentItem(
    val documentId: String,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val isDirectory: Boolean,
    val sizeBytes: Long
)

data class ImportedZipFile(
    val fileName: String,
    val relativePath: String,
    val fileType: StudioFileType,
    val content: String,
    val isBinaryPlaceholder: Boolean
)

class FilePortabilityManager(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver

    fun takePersistableTreePermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            resolver.takePersistableUriPermission(uri, flags)
        }
    }

    suspend fun queryDisplayName(uri: Uri): String = withContext(Dispatchers.IO) {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return@withContext cursor.getString(idx) ?: "archivo.sma"
            }
        }
        uri.lastPathSegment?.substringAfterLast('/') ?: "archivo.sma"
    }

    suspend fun readTextFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            resolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("No se pudo abrir el flujo de lectura para $uri")
        }
    }

    suspend fun writeTextToUri(uri: Uri, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            resolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: error("No se pudo escribir en $uri")
        }
    }

    suspend fun writeBytesToUri(uri: Uri, bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            resolver.openOutputStream(uri, "w")?.use { output ->
                output.write(bytes)
                output.flush()
            } ?: error("No se pudo escribir el archivo binario en $uri")
        }
    }

    suspend fun listDirectory(treeUri: Uri, parentDocId: String? = null): Result<List<SafDocumentItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rootDocId = parentDocId ?: DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                )
                val items = mutableListOf<SafDocumentItem>()
                resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    while (cursor.moveToNext()) {
                        val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: docId else docId
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
                        val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                        val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        items.add(SafDocumentItem(docId, docUri, name, mime, isDir, size))
                    }
                }
                items.sortedWith(compareByDescending<SafDocumentItem> { it.isDirectory }.thenBy { it.name.lowercase() })
            }
        }

    suspend fun exportFileToDirectory(
        treeUri: Uri,
        parentDocId: String?,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val targetDocId = parentDocId ?: DocumentsContract.getTreeDocumentId(treeUri)
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, targetDocId)
            val createdUri = DocumentsContract.createDocument(resolver, parentUri, mimeType, fileName)
                ?: error("No se pudo crear el archivo $fileName en la carpeta seleccionada.")
            resolver.openOutputStream(createdUri, "w")?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: error("Error al abrir flujo de salida hacia $fileName.")
            createdUri
        }
    }

    suspend fun exportProjectToZip(
        outputUri: Uri,
        projectName: String,
        files: List<ProjectFileEntity>,
        compiledBinaries: Map<String, ByteArray> = emptyMap()
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var count = 0
            val safeRoot = projectName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            resolver.openOutputStream(outputUri, "w")?.use { rawOut ->
                ZipOutputStream(rawOut).use { zip ->
                    files.forEach { file ->
                        val entryPath = "$safeRoot/${file.relativePath.trimStart('/')}"
                        zip.putNextEntry(ZipEntry(entryPath))
                        val bytes = compiledBinaries[file.fileName]
                            ?: file.content.toByteArray(Charsets.UTF_8)
                        zip.write(bytes)
                        zip.closeEntry()
                        count++
                    }
                    compiledBinaries.forEach { (binName, bytes) ->
                        if (files.none { it.fileName == binName }) {
                            val entryPath = "$safeRoot/addons/amxmodx/plugins/$binName"
                            zip.putNextEntry(ZipEntry(entryPath))
                            zip.write(bytes)
                            zip.closeEntry()
                            count++
                        }
                    }
                }
            } ?: error("No se pudo escribir el archivo ZIP.")
            count
        }
    }

    suspend fun importProjectFromZip(inputUri: Uri): Result<List<ImportedZipFile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val imported = mutableListOf<ImportedZipFile>()
                resolver.openInputStream(inputUri)?.use { rawIn ->
                    ZipInputStream(rawIn).use { zip ->
                        var entry: ZipEntry? = zip.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val normalized = entry.name.replace('\\', '/').trimStart('/')
                                val fileName = normalized.substringAfterLast('/')
                                val type = StudioFileType.fromFileName(fileName)
                                val buffer = ByteArrayOutputStream()
                                val tmp = ByteArray(4096)
                                var read = zip.read(tmp)
                                while (read > 0) {
                                    buffer.write(tmp, 0, read)
                                    read = zip.read(tmp)
                                }
                                val rawBytes = buffer.toByteArray()
                                val isBinary = type == StudioFileType.AMXX
                                val textContent = if (isBinary) {
                                    "// Archivo binario .amxx importado (${rawBytes.size} bytes)"
                                } else {
                                    rawBytes.toString(Charsets.UTF_8)
                                }
                                imported.add(
                                    ImportedZipFile(
                                        fileName = fileName,
                                        relativePath = normalized,
                                        fileType = type,
                                        content = textContent,
                                        isBinaryPlaceholder = isBinary
                                    )
                                )
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                } ?: error("No se pudo leer el archivo ZIP seleccionado.")
                imported
            }
        }
}
