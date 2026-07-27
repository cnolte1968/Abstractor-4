package com.example.ui.metadata

import com.example.domain.model.DomainSummary

object ExportFormatter {

    fun escapeHtml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    fun formatPlainText(summary: DomainSummary, policy: PresentationPolicy): String {
        return buildString {
            append("**").append(summary.title).append("**\n\n")
            append(summary.shortDescription).append("\n\n")
            summary.keyTakeaways.forEachIndexed { idx, item ->
                val prefix = if (policy.listStyle == ListStyle.NUMBERED) {
                    String.format("%02d. ", idx + 1)
                } else {
                    "• "
                }
                append(prefix).append(item.title).append(": ").append(item.details).append("\n")
            }
        }
    }

    fun formatHtml(summary: DomainSummary, policy: PresentationPolicy): String {
        return buildString {
            append("<html><head><style>")
            append("body { font-family: sans-serif; padding: 20px; color: #333; line-height: 1.6; }")
            append("h1 { color: #1E3A8A; font-size: 24px; border-bottom: 2px solid #E2E8F0; padding-bottom: 10px; }")
            append("h2 { color: #2563EB; font-size: 18px; margin-top: 30px; }")
            append(".url { color: #64748B; font-size: 12px; margin-bottom: 20px; word-break: break-all; }")
            append(".meta { color: #475569; font-size: 14px; margin-bottom: 20px; font-style: italic; }")
            append(".takeaway { background: #F8FAFC; border: 1px solid #E2E8F0; padding: 15px; margin-bottom: 15px; border-radius: 8px; }")
            append(".takeaway-title { font-weight: bold; color: #0F172A; font-size: 14px; margin-bottom: 5px; }")
            append(".takeaway-desc { color: #475569; font-size: 13px; }")
            append("</style></head><body>")

            val safeTitle = escapeHtml(summary.title.ifBlank { "Unbenannter Bericht" })
            append("<h1>").append(safeTitle).append("</h1>")
            append("<div class='url'>Quelle: ").append(escapeHtml(summary.originalUrl)).append("</div>")

            val author = summary.owner
            val authorText = if (!author.isNullOrBlank()) "von ${escapeHtml(author)}" else "Autor nicht eindeutig ermittelbar"
            append("<div class='meta'>").append(authorText).append("</div>")

            append("<h2>Ganz kurz</h2>")
            val safeShortDesc = escapeHtml(summary.shortDescription).replace("\n", "<br/>")
            append("<p>").append(safeShortDesc).append("</p>")

            if (summary.keyTakeaways.isNotEmpty()) {
                append("<h2>Wichtigste Kernaussagen</h2>")
                summary.keyTakeaways.forEachIndexed { index, item ->
                    append("<div class='takeaway'>")
                    val prefix = if (policy.listStyle == ListStyle.NUMBERED) {
                        String.format("%02d. ", index + 1)
                    } else {
                        "&bull; "
                    }
                    val safeItemTitle = escapeHtml(item.title)
                    val safeItemDetails = escapeHtml(item.details).replace("\n", "<br/>")
                    append("<div class='takeaway-title'>").append(prefix).append(safeItemTitle).append("</div>")
                    append("<div class='takeaway-desc'>").append(safeItemDetails).append("</div>")
                    append("</div>")
                }
            }

            append("</body></html>")
        }
    }
}
