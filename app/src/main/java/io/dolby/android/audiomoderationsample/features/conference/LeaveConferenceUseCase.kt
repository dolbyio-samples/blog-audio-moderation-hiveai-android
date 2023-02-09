package io.dolby.android.audiomoderationsample.features.conference

import com.voxeet.VoxeetSDK
import javax.inject.Inject

class LeaveConferenceUseCase @Inject constructor() {

    fun run(onSuccess: (Boolean) -> Unit, onError: (Throwable) -> Unit) = run {
        VoxeetSDK.conference().leave().then{onSuccess(it)}.error{onError(it)}
    }
}