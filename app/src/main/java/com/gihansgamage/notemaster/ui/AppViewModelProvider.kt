package com.gihansgamage.notemaster.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gihansgamage.notemaster.NoteMasterApplication

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NoteMasterApplication
            NoteMasterViewModel(
                repository = application.container.noteRepository,
                preferencesRepository = application.container.userPreferencesRepository,
                summarizer = application.container.noteSummarizer,
            )
        }
    }
}
