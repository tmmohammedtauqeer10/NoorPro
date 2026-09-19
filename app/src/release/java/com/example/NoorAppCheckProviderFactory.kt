package com.example

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object NoorAppCheckProviderFactory {
    fun get(): AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
}
