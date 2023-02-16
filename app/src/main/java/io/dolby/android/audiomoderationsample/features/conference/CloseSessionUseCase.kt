package io.dolby.android.audiomoderationsample.features.conference

import com.voxeet.VoxeetSDK
import javax.inject.Inject

class CloseSessionUseCase @Inject constructor() {
    fun run(onSuccess: (Boolean) -> Unit, onError: (Throwable) -> Unit) =
        VoxeetSDK.session().close().then{onSuccess(it)}.error{onError(it)}
}