package com.rimagwinya.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt needs one of these to build the dependency graph. Everything else the
 * app does at startup should stay out of here — a slow Application class is a
 * slow cold start on exactly the budget phones this app has to run on.
 */
@HiltAndroidApp
class RimagwinyaApp : Application()
