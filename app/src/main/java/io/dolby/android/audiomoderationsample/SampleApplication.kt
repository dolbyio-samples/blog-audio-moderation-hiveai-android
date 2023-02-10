package io.dolby.android.audiomoderationsample

import io.dolby.android.audiomoderationsample.coroutines.ApplicationScope
import android.app.Application
import com.voxeet.audio.utils.Log
import dagger.hilt.android.HiltAndroidApp
import io.dolby.android.audiomoderationsample.coroutines.launch
import io.dolby.android.audiomoderationsample.sdk.InitSdk
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class SampleApplication : Application() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var initSdk: InitSdk

    private val originalUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun onCreate() {
        super.onCreate()

        Log.enable(true);

        applicationScope.launch {
            initSdk.initializeSDK { Configuration.DOLBY_IO_TOKEN }
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("Application crashed! Thread: $thread", exception.message)
            originalUncaughtExceptionHandler?.uncaughtException(thread, exception) // Handle exception like always
        }
    }
}