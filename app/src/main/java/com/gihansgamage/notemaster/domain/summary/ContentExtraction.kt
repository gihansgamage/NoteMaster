package com.gihansgamage.notemaster.domain.summary

data class ExtractedContent(
    val sourceLabel: String,
    val text: String,
)

interface PdfTextExtractor {
    suspend fun extract(uri: String): ExtractedContent?
}

interface ImageTextExtractor {
    suspend fun extract(uri: String): ExtractedContent?
}

interface AudioTranscriptExtractor {
    suspend fun extract(uri: String): ExtractedContent?
}

interface DocumentTextExtractor {
    suspend fun extract(uri: String): ExtractedContent?
}

interface AiSummaryService {
    suspend fun summarize(
        title: String,
        body: String,
        extractedContent: List<ExtractedContent>,
        tags: List<String>,
    ): String
}

class NoOpPdfTextExtractor : PdfTextExtractor {
    override suspend fun extract(uri: String): ExtractedContent? = null
}

class NoOpImageTextExtractor : ImageTextExtractor {
    override suspend fun extract(uri: String): ExtractedContent? = null
}

class NoOpAudioTranscriptExtractor : AudioTranscriptExtractor {
    override suspend fun extract(uri: String): ExtractedContent? = null
}

class NoOpDocumentTextExtractor : DocumentTextExtractor {
    override suspend fun extract(uri: String): ExtractedContent? = null
}
