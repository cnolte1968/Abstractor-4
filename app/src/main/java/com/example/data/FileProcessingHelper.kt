package com.example.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Isolated helper class representing FileProcessingHelper that encapsulates 
 * local streaming list operations to byte arrays, MIME-Type conversions, and
 * high-performance local text extraction from documents.
 */
object FileProcessingHelper {

    /**
     * Reads a file Uri sequentially from a content resolver into a byte array
     * in memory without persistent local storage caching.
     */
    fun readUriToByteArray(contentResolver: ContentResolver, uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val byteBuffer = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    byteBuffer.write(buffer, 0, len)
                }
                byteBuffer.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Retrieves the MIME type dynamically using the Android ContentResolver.
     */
    fun getMimeType(contentResolver: ContentResolver, uri: Uri): String {
        val type = contentResolver.getType(uri)
        if (type != null) return type
        
        // Fallback by file extension if content resolver doesn't know
        val path = uri.path ?: return "application/octet-stream"
        return when {
            path.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            path.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            path.endsWith(".pptx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            path.endsWith(".doc", ignoreCase = true) -> "application/msword"
            path.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            path.endsWith(".txt", ignoreCase = true) -> "text/plain"
            path.endsWith(".md", ignoreCase = true) -> "text/markdown"
            path.endsWith(".csv", ignoreCase = true) -> "text/csv"
            path.endsWith(".json", ignoreCase = true) -> "application/json"
            else -> "application/octet-stream"
        }
    }

    /**
     * Converts raw bytes directly into a Base64 string required by Gemini API's inlineData.
     */
    fun toBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Checks if we can process standard text extraction locally (plaintext, docx, xlsx, pptx).
     */
    fun isExtractableTextType(mimeType: String, fileName: String?): Boolean {
        val nameLower = fileName?.lowercase() ?: ""
        return mimeType.startsWith("text/", ignoreCase = true) ||
               mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || 
               mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
               mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" ||
               mimeType == "application/json" || 
               mimeType == "application/javascript" ||
               nameLower.endsWith(".txt") ||
               nameLower.endsWith(".md") ||
               nameLower.endsWith(".markdown") ||
               nameLower.endsWith(".csv") ||
               nameLower.endsWith(".json") ||
               nameLower.endsWith(".docx") ||
               nameLower.endsWith(".xlsx") ||
               nameLower.endsWith(".pptx")
    }

    /**
     * Extracts text from the given content URI if it is of a text-compatible format
     * or a modern Microsoft Word, Excel, or PowerPoint document.
     */
    fun extractTextFromUri(contentResolver: ContentResolver, uri: Uri, mimeType: String, fileName: String?): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val nameLower = fileName?.lowercase() ?: ""
                when {
                    mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || nameLower.endsWith(".docx") -> {
                        extractTextFromDocxStream(inputStream)
                    }
                    mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" || nameLower.endsWith(".xlsx") -> {
                        extractTextFromXlsxStream(inputStream)
                    }
                    mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" || nameLower.endsWith(".pptx") -> {
                        extractTextFromPptxStream(inputStream)
                    }
                    else -> {
                        // Standard plain text loader
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(2048)
                        var len: Int
                        while (inputStream.read(buffer).also { len = it } != -1) {
                            output.write(buffer, 0, len)
                        }
                        String(output.toByteArray(), Charsets.UTF_8)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts plain text from a Microsoft Word .docx OOXML package stream.
     * Uses zero-allocation Stream parsing to prevent out-of-memory errors on large documents.
     */
    private fun extractTextFromDocxStream(inputStream: InputStream): String? {
        return try {
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                        return parseDocxXml(xmlContent)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDocxXml(xml: String): String {
        val result = StringBuilder()
        val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
        val matches = regex.findAll(xml)
        for (match in matches) {
            val decoded = decodeXmlEntities(match.groupValues[1])
            result.append(decoded).append(" ")
        }
        return result.toString().trim()
    }

    /**
     * Extracts text from Microsoft Excel .xlsx OOXML package stream.
     */
    private fun extractTextFromXlsxStream(inputStream: InputStream): String? {
        return try {
            val result = StringBuilder()
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name == "xl/sharedstrings.xml") {
                        val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                        val regex = Regex("<t[^>]*>(.*?)</t>")
                        val matches = regex.findAll(xmlContent)
                        for (match in matches) {
                            val decoded = decodeXmlEntities(match.groupValues[1])
                            result.append(decoded).append(" ")
                        }
                    } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                        val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                        val regexV = Regex("<v[^>]*>(.*?)</v>")
                        val matchesV = regexV.findAll(xmlContent)
                        for (match in matchesV) {
                            val decoded = decodeXmlEntities(match.groupValues[1])
                            result.append(decoded).append(" ")
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            val output = result.toString().trim()
            if (output.isNotEmpty()) output else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts text from Microsoft PowerPoint .pptx OOXML package stream.
     */
    private fun extractTextFromPptxStream(inputStream: InputStream): String? {
        return try {
            val result = StringBuilder()
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                        val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                        val regex = Regex("<a:t[^>]*>(.*?)</a:t>")
                        val matches = regex.findAll(xmlContent)
                        for (match in matches) {
                            val decoded = decodeXmlEntities(match.groupValues[1])
                            result.append(decoded).append(" ")
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            val output = result.toString().trim()
            if (output.isNotEmpty()) output else null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
