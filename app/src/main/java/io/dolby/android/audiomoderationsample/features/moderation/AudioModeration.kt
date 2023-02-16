package io.dolby.android.audiomoderationsample.features.moderation

import io.dolby.android.audiomoderationsample.Configuration
import io.dolby.android.audiomoderationsample.features.moderation.model.ModerationResponse

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

interface AudioModeration {
    suspend fun getModeration(waveFile: File): ModerationResponse
}

class AudioModerationImpl @Inject constructor(
    private val audioModerationApi: AudioModerationApi
) : AudioModeration {

    override suspend fun getModeration(waveFile: File): ModerationResponse {

        // See https://docs.thehive.ai/reference/submit-a-task-synchronously
        // for Hive documentation

        val body = waveFile.asRequestBody("audio/*".toMediaTypeOrNull())

        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("media", waveFile.name, body)
            .build()

        return audioModerationApi.moderate("token " + Configuration.HIVE_API_KEY, requestBody)
    }

}
