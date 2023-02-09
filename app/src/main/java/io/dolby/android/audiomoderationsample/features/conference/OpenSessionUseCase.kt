package io.dolby.android.audiomoderationsample.features.conference

import com.voxeet.VoxeetSDK
import com.voxeet.sdk.json.ParticipantInfo
import javax.inject.Inject

class OpenSessionUseCase @Inject constructor() {
    fun run(username: String, externalId: String, onSuccess: (Boolean) -> Unit, onError: (Throwable) -> Unit) = run {
        VoxeetSDK.session().open(ParticipantInfo(username, externalId, "")).then {
            onSuccess(it)
        }.error {
            onError(it)
        }
    }
}