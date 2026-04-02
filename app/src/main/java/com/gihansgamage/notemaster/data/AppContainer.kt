package com.gihansgamage.notemaster.data

import android.content.Context
import com.gihansgamage.notemaster.data.local.NoteMasterDatabase
import com.gihansgamage.notemaster.data.repository.NoteRepository
import com.gihansgamage.notemaster.data.repository.OfflineNoteRepository
import com.gihansgamage.notemaster.domain.summary.HeuristicNoteSummarizer
import com.gihansgamage.notemaster.domain.summary.NoteSummarizer

class AppContainer(context: Context) {
    private val database: NoteMasterDatabase by lazy {
        NoteMasterDatabase.getDatabase(context)
    }

    val noteSummarizer: NoteSummarizer by lazy {
        HeuristicNoteSummarizer()
    }

    val noteRepository: NoteRepository by lazy {
        OfflineNoteRepository(
            database = database,
            summarizer = noteSummarizer,
        )
    }
}
