package com.gihansgamage.notemaster.util

data class TocItem(
    val title: String,
    val depth: Int,
)

object TableOfContentsParser {
    fun parse(body: String): List<TocItem> {
        val headings = body.lineSequence().mapNotNull { line ->
            when {
                line.startsWith("### ") -> TocItem(line.removePrefix("### ").trim(), 3)
                line.startsWith("## ") -> TocItem(line.removePrefix("## ").trim(), 2)
                line.startsWith("# ") -> TocItem(line.removePrefix("# ").trim(), 1)
                Regex("^\\d+\\.\\s+").containsMatchIn(line) -> {
                    TocItem(line.replaceFirst(Regex("^\\d+\\.\\s+"), "").trim(), 1)
                }

                else -> null
            }
        }.filter { it.title.isNotBlank() }.toList()

        if (headings.isNotEmpty()) return headings

        return body.lineSequence()
            .filter { it.isNotBlank() }
            .take(4)
            .map { TocItem(it.take(36), 1) }
            .toList()
    }
}
