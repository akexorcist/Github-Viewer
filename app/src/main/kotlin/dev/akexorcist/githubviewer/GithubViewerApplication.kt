package dev.akexorcist.githubviewer

import android.app.Application
import dev.akexorcist.githubviewer.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GithubViewerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GithubViewerApplication)
            modules(appModules)
        }
    }
}
