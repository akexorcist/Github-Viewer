package dev.akexorcist.githubviewer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import dev.akexorcist.githubviewer.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GithubViewerApplication : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GithubViewerApplication)
            modules(appModules)
        }
    }
}
