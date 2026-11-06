package com.anadcurukovic.berylux

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt builds the app-wide object graph from here: database, settings, repositories. */
@HiltAndroidApp
class BeryluxApplication : Application()
