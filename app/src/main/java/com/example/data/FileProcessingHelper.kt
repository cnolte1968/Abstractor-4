package com.example.data

import android.content.ContentResolver
import android.net.Uri
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.SourceType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object FileProcessingHelper {
    fun getMimeType(contentResolver: ContentResolver, uri: Uri): String {
        return contentResolver.getType(uri) ?: "application/octet-stream"
    }

    fun isExtractableTextType(mimeType: String, fileName: String): Boolean {
        val lower = fileName.lowercase()
        return mimeType.startsWith("text/") ||
               mimeType == "application/pdf" ||
               lower.endsWith(".txt") ||
               lower.endsWith(".md") ||
               lower.endsWith(".csv") ||
               lower.endsWith(".json") ||
               lower.endsWith(".xml") ||
               lower.endsWith(".html") ||
               lower.endsWith(".docx") ||
               lower.endsWith(".xlsx") ||
               lower.endsWith(".pptx") ||
               lower.endsWith(".pdf")
    }

    fun extractFileContent(contentResolver: ContentResolver, uri: Uri, mimeType: String, fileName: String, analysisId: String = java.util.UUID.randomUUID().toString()): CanonicalAnalysisInput {
        val text = extractTextFromUri(contentResolver, uri, mimeType, fileName)
        if (text.isNullOrBlank()) {
            throw java.io.IOException("No text content could be extracted from file $fileName")
        }
        return CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = text,
            enrichedText = text,
            metadata = mapOf(
                "fileName" to fileName,
                "mimeType" to mimeType,
                "uri" to uri.toString()
            ),
            analysisId = analysisId
        )
    }

    fun extractTextFromUri(contentResolver: ContentResolver, uri: Uri, mimeType: String, fileName: String): String? {
        return try {
            val lower = fileName.lowercase()
            if (lower.endsWith(".docx") || lower.endsWith(".xlsx") || lower.endsWith(".pptx")) {
                extractOfficeText(contentResolver, uri)
            } else if (lower.endsWith(".pdf") || mimeType == "application/pdf") {
                val bytes = readUriToByteArray(contentResolver, uri)
                if (bytes != null) {
                    extractTextFromPdf(bytes)
                } else {
                    null
                }
            } else {
                extractPlaintext(contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractPlaintext(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                String(bytes, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun extractOfficeTextFromBytes(bytes: ByteArray): String? {
        return try {
            val builder = StringBuilder()
            java.io.ByteArrayInputStream(bytes).use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".xml")) {
                            val xmlContent = zip.bufferedReader().readText()
                            val cleaned = xmlContent.replace(Regex("<[^>]*?>"), " ")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                            if (cleaned.isNotBlank() && !entry.name.contains("[Content_Types]")) {
                                builder.append(cleaned).append("\n")
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            builder.toString().trim().ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractOfficeText(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                extractOfficeTextFromBytes(bytes)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun readUriToByteArray(contentResolver: ContentResolver, uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun extractTextFromPdf(bytes: ByteArray): String? {
        val tag = "PDF_EXTRACTION"
        android.util.Log.d(tag, "Starting PDF text extraction. Total bytes: ${bytes.size}")
        
        val textBuilder = java.lang.StringBuilder()
        var offset = 0
        val streamPattern = "stream".toByteArray(Charsets.US_ASCII)
        val endstreamPattern = "endstream".toByteArray(Charsets.US_ASCII)
        
        var totalStreams = 0
        var skippedStreams = 0
        var processedStreams = 0

        while (true) {
            val streamIndex = findBytePattern(bytes, streamPattern, offset)
            if (streamIndex == -1) break

            totalStreams++
            var streamDataStart = streamIndex + 6
            while (streamDataStart < bytes.size && (bytes[streamDataStart] == '\r'.code.toByte() || bytes[streamDataStart] == '\n'.code.toByte())) {
                streamDataStart++
            }

            val endstreamIndex = findBytePattern(bytes, endstreamPattern, streamDataStart)
            if (endstreamIndex == -1) {
                android.util.Log.d(tag, "Stream #$totalStreams has no matching endstream. Skipping.")
                offset = streamIndex + 6
                continue
            }

            val streamData = bytes.sliceArray(streamDataStart until endstreamIndex)
            val decompressed = decompressFlate(streamData)
            val streamToParse = decompressed ?: streamData

            // Skip font/encoding metadata streams entirely
            val contentStr = try {
                String(streamToParse, 0, minOf(streamToParse.size, 1000), Charsets.US_ASCII)
            } catch (e: Exception) {
                ""
            }
            
            val hasCMap = contentStr.contains("begincmap") || contentStr.contains("CMapName")
            val hasCID = contentStr.contains("CIDInit") || contentStr.contains("CIDSystemInfo")
            val hasAdobe = contentStr.contains("Adobe-Identity-UCS") || contentStr.contains("begincodespacerange")
            val hasFont = contentStr.contains("/FontDescriptor") || contentStr.contains("/ToUnicode") || contentStr.contains("/FontName")
            val hasIdentity = contentStr.contains("Identity-H") || contentStr.contains("Identity-V")
            
            val isFontOrCMap = hasCMap || hasCID || hasAdobe || hasFont || hasIdentity

            if (isFontOrCMap) {
                skippedStreams++
                val reason = when {
                    hasCMap -> "CMap metadata"
                    hasCID -> "CIDInit structures"
                    hasAdobe -> "Adobe UCS ranges"
                    hasFont -> "Font descriptors"
                    hasIdentity -> "Identity font settings"
                    else -> "Metadata keywords"
                }
                android.util.Log.d(tag, "Stream #$totalStreams skipped. Reason: $reason. Preview: ${contentStr.take(60).replace("\n", " ")}")
            } else {
                processedStreams++
                val extractedText = extractTextFromStream(streamToParse)
                val wordsCount = extractedText.split(Regex("\\s+")).count { it.isNotBlank() }
                android.util.Log.d(tag, "Stream #$totalStreams parsed as content. Chars: ${extractedText.length}, Words: $wordsCount")
                if (extractedText.isNotBlank()) {
                    textBuilder.append(extractedText).append("\n")
                }
            }

            offset = endstreamIndex + 9
        }

        val result = textBuilder.toString().trim()
        android.util.Log.d(tag, "Extraction pass complete. Total streams found: $totalStreams, Skipped (Metadata): $skippedStreams, Processed: $processedStreams. Raw combined length: ${result.length}")
        
        if (result.isBlank()) {
            android.util.Log.e(tag, "Extraction failed: Result text is completely blank.")
            return null
        }
        
        if (isResidueOrGarbage(result)) {
            android.util.Log.e(tag, "Extraction failed: Extracted text classified as font-encoding residue or unreadable garbage.")
            return null
        }
        
        android.util.Log.i(tag, "Extraction succeeded! Text is clean and readable. Words: ${result.split(Regex("\\s+")).filter { it.isNotBlank() }.size}")
        return result
    }

    private fun isResidueOrGarbage(text: String): Boolean {
        val tag = "PDF_EXTRACTION"
        val lowerText = text.lowercase()
        val blacklist = listOf(
            "adobe ucs",
            "adobe-identity-ucs",
            "begincmap",
            "cmapname",
            "cidinit",
            "cidsysteminfo",
            "begincodespacerange",
            "identity-h",
            "identity-v",
            "tounicode",
            "fontdescriptor",
            "fontname"
        )
        for (keyword in blacklist) {
            if (lowerText.contains(keyword)) {
                android.util.Log.d(tag, "Garbage check: Text contains blacklisted keyword '$keyword'.")
                return true
            }
        }

        if (text.length < 30) {
            android.util.Log.d(tag, "Garbage check: Text length is too short (${text.length} < 30).")
            return true
        }

        var totalChars = 0
        var letterChars = 0
        var vowelChars = 0
        
        for (char in lowerText) {
            totalChars++
            if (char.isLetter()) {
                letterChars++
                if (char in "aeiouäöü") {
                    vowelChars++
                }
            }
        }
        
        if (totalChars == 0) {
            android.util.Log.d(tag, "Garbage check: No characters found.")
            return true
        }
        
        val letterRatio = letterChars.toFloat() / totalChars
        if (letterRatio < 0.25f) {
            android.util.Log.d(tag, "Garbage check: Letter ratio is too low ($letterRatio < 0.25). Likely raw data or glyph codes.")
            return true
        }
        
        if (letterChars > 0) {
            val vowelRatio = vowelChars.toFloat() / letterChars
            if (vowelRatio < 0.15f || vowelRatio > 0.65f) {
                android.util.Log.d(tag, "Garbage check: Vowel ratio of letters is unnatural ($vowelRatio is outside 0.15..0.65).")
                return true
            }
        } else {
            android.util.Log.d(tag, "Garbage check: No letters found.")
            return true
        }
        
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) {
            android.util.Log.d(tag, "Garbage check: No words found.")
            return true
        }
        
        val avgWordLength = words.map { it.length }.average()
        if (avgWordLength < 2.0 || avgWordLength > 18.0) {
            android.util.Log.d(tag, "Garbage check: Average word length is abnormal ($avgWordLength is outside 2.0..18.0).")
            return true
        }
        
        if (words.size < 4) {
            android.util.Log.d(tag, "Garbage check: Too few words (${words.size} < 4).")
            return true
        }

        return false
    }

    private fun findBytePattern(bytes: ByteArray, pattern: ByteArray, startIndex: Int): Int {
        if (pattern.isEmpty() || startIndex < 0 || startIndex > bytes.size - pattern.size) return -1
        for (i in startIndex..bytes.size - pattern.size) {
            var match = true
            for (j in pattern.indices) {
                if (bytes[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun decompressFlate(data: ByteArray): ByteArray? {
        val decompressor = java.util.zip.Inflater()
        decompressor.setInput(data)
        val outputStream = java.io.ByteArrayOutputStream(data.size)
        val buffer = ByteArray(1024)
        return try {
            while (!decompressor.finished()) {
                val count = decompressor.inflate(buffer)
                if (count == 0 && decompressor.needsInput()) {
                    break
                }
                outputStream.write(buffer, 0, count)
            }
            decompressor.end()
            outputStream.toByteArray()
        } catch (e: Exception) {
            decompressor.end()
            null
        }
    }

    private fun extractTextFromStream(streamBytes: ByteArray): String {
        val outputBytes = java.io.ByteArrayOutputStream()
        var inParens = false
        var inHex = false
        var escape = false
        var i = 0
        val size = streamBytes.size
        
        val hexBuffer = java.lang.StringBuilder()
        
        while (i < size) {
            val b = streamBytes[i]
            val c = b.toInt().toChar()
            
            if (escape) {
                if (c in '0'..'7') {
                    var octalVal = c - '0'
                    if (i + 1 < size && streamBytes[i + 1].toInt().toChar() in '0'..'7') {
                        i++
                        octalVal = octalVal * 8 + (streamBytes[i].toInt().toChar() - '0')
                        if (i + 1 < size && streamBytes[i + 1].toInt().toChar() in '0'..'7') {
                            i++
                            octalVal = octalVal * 8 + (streamBytes[i].toInt().toChar() - '0')
                        }
                    }
                    outputBytes.write(octalVal)
                } else {
                    when (c) {
                        'n' -> outputBytes.write('\n'.code)
                        'r' -> outputBytes.write('\r'.code)
                        't' -> outputBytes.write('\t'.code)
                        'b' -> outputBytes.write('\b'.code)
                        'f' -> outputBytes.write('\u000C'.code)
                        else -> outputBytes.write(b.toInt())
                    }
                }
                escape = false
            } else if (c == '\\' && inParens) {
                escape = true
            } else if (c == '(' && !inParens && !inHex) {
                inParens = true
            } else if (c == ')' && inParens) {
                inParens = false
                outputBytes.write(' '.code)
            } else if (inParens) {
                outputBytes.write(b.toInt())
            } else if (c == '<' && !inParens && !inHex) {
                // Check if this is the start of a << dictionary
                if (i + 1 < size && streamBytes[i + 1].toInt().toChar() == '<') {
                    i++ // Skip both characters
                } else {
                    inHex = true
                    hexBuffer.setLength(0)
                }
            } else if (c == '>' && inHex) {
                inHex = false
                val hexStr = hexBuffer.toString().replace(Regex("\\s+"), "")
                if (hexStr.length >= 2 && hexStr.all { it in "0123456789abcdefABCDEF" }) {
                    var h = 0
                    while (h + 1 < hexStr.length) {
                        try {
                            val byteVal = hexStr.substring(h, h + 2).toInt(16)
                            outputBytes.write(byteVal)
                        } catch (e: Exception) {
                            // Suppress and break on invalid hex values
                        }
                        h += 2
                    }
                    outputBytes.write(' '.code)
                }
            } else if (inHex) {
                if (c in "0123456789abcdefABCDEF \t\r\n") {
                    hexBuffer.append(c)
                } else {
                    inHex = false
                }
            }
            i++
        }
        
        val rawBytes = outputBytes.toByteArray()
        val decoded = try {
            if (rawBytes.size >= 2 && rawBytes[0] == 0xFE.toByte() && rawBytes[1] == 0xFF.toByte()) {
                String(rawBytes, 2, rawBytes.size - 2, Charsets.UTF_16BE)
            } else {
                String(rawBytes, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            String(rawBytes)
        }
        return cleanExtractedText(decoded)
    }

    private fun cleanExtractedText(text: String): String {
        val sb = java.lang.StringBuilder()
        for (char in text) {
            if (char.code in 32..126 || char == '\n' || char == '\r' || char == '\t' || char.code in 160..255 || char.code > 255) {
                sb.append(char)
            }
        }
        return sb.toString().replace(Regex("[ \t]+"), " ").trim()
    }
}
