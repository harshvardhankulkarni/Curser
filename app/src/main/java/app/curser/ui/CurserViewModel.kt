package app.curser.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.curser.core.TrackpadController

/** Holds the single TrackpadController so it survives configuration changes. */
class CurserViewModel(application: Application) : AndroidViewModel(application) {
    val controller = TrackpadController(application.applicationContext)
}
