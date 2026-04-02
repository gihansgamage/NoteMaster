package com.gihansgamage.notemaster

import android.app.Application
import com.gihansgamage.notemaster.data.AppContainer

class NoteMasterApplication : Application() {
    val container: AppContainer by lazy { AppContainer(applicationContext) }
}
