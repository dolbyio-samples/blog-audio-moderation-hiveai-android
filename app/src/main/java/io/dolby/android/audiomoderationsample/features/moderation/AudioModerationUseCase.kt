package io.dolby.android.audiomoderationsample.features.moderation

import io.dolby.android.audiomoderationsample.features.moderation.model.ModerationResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AudioModerationUseCase @Inject constructor(
    private val audioModeration: AudioModeration
) {

    fun run(waveFile: File, onSuccess: (ModerationResponse) -> Unit, onError: (Throwable) -> Unit) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val mod = audioModeration.getModeration(waveFile)
            onSuccess(mod)
        } catch (e: java.lang.Exception) {
            onError(e)
        }
    }
}