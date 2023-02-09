package io.dolby.android.audiomoderationsample.features.moderation

import io.dolby.android.audiomoderationsample.features.moderation.model.ModerationResponse
import okhttp3.RequestBody
import retrofit2.http.*

interface AudioModerationApi {
    @POST("api/v2/task/sync")
    @Headers("accept: application/json")
    suspend fun moderate(@Header("authorization") authorization: String, @Body body: RequestBody): ModerationResponse
}
