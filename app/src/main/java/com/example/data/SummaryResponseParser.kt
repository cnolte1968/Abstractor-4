package com.example.data

import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class DomainSummaryAdapter(private val moshi: Moshi) : JsonAdapter<DomainSummary>() {
    private val takeawayListType = Types.newParameterizedType(List::class.java, TakeawayItem::class.java)
    private val takeawaysAdapter: JsonAdapter<List<TakeawayItem>> by lazy {
        moshi.adapter(takeawayListType)
    }

    override fun fromJson(reader: JsonReader): DomainSummary? {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any>()
            return null
        }
        var id = java.util.UUID.randomUUID().toString()
        var title = ""
        var originalUrl = ""
        var shortDescription = ""
        var keyTakeaways = emptyList<TakeawayItem>()
        var owner: String? = null
        var timestamp = System.currentTimeMillis()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "title" -> title = reader.nextString()
                "original_url" -> originalUrl = reader.nextString()
                "short_description" -> shortDescription = reader.nextString()
                "key_takeaways" -> keyTakeaways = takeawaysAdapter.fromJson(reader) ?: emptyList()
                "owner" -> {
                    if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Any>()
                        owner = null
                    } else {
                        owner = reader.nextString()
                    }
                }
                "timestamp" -> timestamp = reader.nextLong()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return DomainSummary(
            id = id,
            title = title,
            originalUrl = originalUrl,
            shortDescription = shortDescription,
            keyTakeaways = keyTakeaways,
            owner = owner,
            timestamp = timestamp
        )
    }

    override fun toJson(writer: JsonWriter, value: DomainSummary?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("id").value(value.id)
        writer.name("title").value(value.title)
        writer.name("original_url").value(value.originalUrl)
        writer.name("short_description").value(value.shortDescription)
        writer.name("key_takeaways")
        takeawaysAdapter.toJson(writer, value.keyTakeaways)
        writer.name("owner").value(value.owner)
        writer.name("timestamp").value(value.timestamp)
        writer.endObject()
    }
}

class TakeawayItemAdapter : JsonAdapter<TakeawayItem>() {
    override fun fromJson(reader: JsonReader): TakeawayItem? {
        val peek = reader.peek()
        return when (peek) {
            JsonReader.Token.BEGIN_OBJECT -> {
                var title = ""
                var details = ""
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "title" -> title = reader.nextString()
                        "details" -> details = reader.nextString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                TakeawayItem(title = title, details = details)
            }
            JsonReader.Token.STRING -> {
                val str = reader.nextString()
                parseLegacyString(str)
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: TakeawayItem?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("title").value(value.title)
        writer.name("details").value(value.details)
        writer.endObject()
    }

    private fun parseLegacyString(str: String): TakeawayItem {
        val trimmed = str.trim()
        var title = ""
        var details = ""

        if (trimmed.startsWith("**")) {
            val closingIdx = trimmed.indexOf("**", 2)
            if (closingIdx != -1) {
                title = trimmed.substring(2, closingIdx).trim()
                var rest = trimmed.substring(closingIdx + 2).trim()
                if (rest.startsWith(":")) {
                    rest = rest.substring(1).trim()
                }
                details = rest
            }
        }

        if (title.isEmpty() || details.isEmpty()) {
            val colonIdx = trimmed.indexOf(":")
            if (colonIdx != -1 && colonIdx > 0 && colonIdx < trimmed.length - 1) {
                val candidateTitle = trimmed.substring(0, colonIdx).trim()
                val cleanTitle = candidateTitle.removePrefix("**").removeSuffix("**").trim()
                val candidateDetails = trimmed.substring(colonIdx + 1).trim()

                if (cleanTitle.isNotEmpty() && candidateDetails.isNotEmpty()) {
                    title = cleanTitle
                    details = candidateDetails
                }
            }
        }

        if (title.isEmpty() || details.isEmpty()) {
            title = "Inhalt"
            details = trimmed
        }

        return TakeawayItem(title = title, details = details)
    }
}

object SummaryResponseParser {
    val moshiInstance: Moshi = Moshi.Builder()
        .add(TakeawayItem::class.java, TakeawayItemAdapter())
        .add { type, _, moshi ->
            if (type == DomainSummary::class.java) {
                DomainSummaryAdapter(moshi)
            } else {
                null
            }
        }
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun parse(input: String): DomainSummary {
        var json = input.trim()
        if (json.startsWith("```")) {
            val lines = json.split("\n")
            val cleanLines = lines.filter { !it.trim().startsWith("```") }
            json = cleanLines.joinToString("\n").trim()
        }
        val firstBrace = json.indexOf('{')
        val lastBrace = json.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            json = json.substring(firstBrace, lastBrace + 1)
        }

        try {
            return moshiInstance.adapter(DomainSummary::class.java).fromJson(json)
                ?: throw IllegalArgumentException("Die Antwort konnte nicht verarbeitet werden: Das Antwortformat ist ungültig oder leer.")
        } catch (e: Exception) {
            throw IllegalArgumentException("Die Antwort konnte nicht verarbeitet werden: Das Antwortformat ist ungültig oder unvollständig.", e)
        }
    }
}
