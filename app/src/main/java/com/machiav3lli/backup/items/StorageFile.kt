package com.machiav3lli.backup.items

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.machiav3lli.backup.Constants.classTag
import com.machiav3lli.backup.handler.DocumentContractApi
import java.io.FileNotFoundException
import java.util.*

open class StorageFile protected constructor(val parentFile: StorageFile?, private val context: Context, var uri: Uri) {
    var name: String? = null
        get() {
            if (field == null) field = DocumentContractApi.getName(context, uri)
            return field
        }
        private set

    val isFile: Boolean
        get() = DocumentContractApi.isFile(context, this.uri)

    val isPropertyFile: Boolean
        get() = DocumentContractApi.isPropertyFile(context, this.uri)

    val isDirectory: Boolean
        get() = DocumentContractApi.isDirectory(context, this.uri)

    fun createDirectory(displayName: String): StorageFile? {
        val result = createFile(context, uri, DocumentsContract.Document.MIME_TYPE_DIR, displayName)
        return if (result != null) StorageFile(this, context, result) else null
    }

    fun createFile(mimeType: String, displayName: String): StorageFile? {
        val result = createFile(context, uri, mimeType, displayName)
        return if (result != null) StorageFile(this, context, result) else null
    }

    fun delete(): Boolean {
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: FileNotFoundException) {
            false
        }
    }

    fun findFile(displayName: String): StorageFile? {
        try {
            for (doc in listFiles()) {
                if (displayName == doc.name) {
                    return doc
                }
            }
        } catch (e: FileNotFoundException) {
            return null
        }
        return null
    }

    // TODO cause of huge part of cpu time
    @Throws(FileNotFoundException::class)
    fun listFiles(): Array<StorageFile> {
        if (!exists()) {
            throw FileNotFoundException("File $uri does not exist")
        }
        val uriString = this.uri.toString()
        if (cacheDirty) {
            cacheDirty = false
        }
        if (cache[uriString].isNullOrEmpty()) {
            val resolver = context.contentResolver
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(this.uri,
                    DocumentsContract.getDocumentId(this.uri))
            val results = ArrayList<Uri>()
            var cursor: Cursor? = null
            try {
                cursor = resolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                        null, null, null)
                var documentUri: Uri
                while (cursor!!.moveToNext()) {
                    documentUri = DocumentsContract.buildDocumentUriUsingTree(this.uri, cursor.getString(0))
                    results.add(documentUri)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed query: $e")
            } finally {
                closeQuietly(cursor)
            }
            cache[uriString] = results.map { uri ->
                StorageFile(this, context, uri)
            }.toTypedArray()
        }
        return cache[uriString] ?: arrayOf()
    }

    fun renameTo(displayName: String?): Boolean {
        // noinspection OverlyBroadCatchBlock
        return try {
            val result = DocumentsContract.renameDocument(
                    context.contentResolver, uri, displayName!!)
            if (result != null) {
                uri = result
                return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun exists(): Boolean {
        return DocumentContractApi.exists(context, uri)
    }

    override fun toString(): String {
        return DocumentsContract.getDocumentId(uri)
    }

    companion object {
        val TAG = classTag(".StorageFile")
        val cache: MutableMap<String, Array<StorageFile>> = mutableMapOf()
        var cacheDirty = true

        fun fromUri(context: Context, uri: Uri): StorageFile {
            // Todo: Figure out what's wrong with the Uris coming from the intent and why they need to be processed with DocumentsContract.buildDocumentUriUsingTree(value, DocumentsContract.getTreeDocumentId(value)) first
            return StorageFile(null, context, uri)
        }

        fun createFile(context: Context, self: Uri?, mimeType: String?, displayName: String?): Uri? {
            return try {
                DocumentsContract.createDocument(context.contentResolver, self!!, mimeType!!, displayName!!)
            } catch (e: FileNotFoundException) {
                null
            }
        }

        fun invalidateCache() {
            cacheDirty = true
        }

        private fun closeQuietly(closeable: AutoCloseable?) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (rethrown: RuntimeException) {
                    // noinspection ProhibitedExceptionThrown
                    throw rethrown
                } catch (ignored: Exception) {
                }
            }
        }
    }
}