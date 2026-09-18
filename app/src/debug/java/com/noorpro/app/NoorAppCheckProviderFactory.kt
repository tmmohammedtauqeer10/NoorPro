package com.noorpro.app

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object NoorAppCheckProviderFactory {
    fun get(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
