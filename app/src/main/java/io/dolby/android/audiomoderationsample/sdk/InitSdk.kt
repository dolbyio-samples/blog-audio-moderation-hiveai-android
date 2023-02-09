package io.dolby.android.audiomoderationsample.sdk

import ApplicationScope
import com.voxeet.VoxeetSDK
import com.voxeet.sdk.authent.token.TokenCallback
import kotlinx.coroutines.CoroutineScope
import io.dolby.android.audiomoderationsample.coroutines.launch
import javax.inject.Inject

class InitSdk @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    fun initialize(accessToken: String, callback: (Boolean, TokenCallback) -> Unit) {
        VoxeetSDK.initialize(accessToken, callback)
    }

    suspend fun initializeSDK(refreshToken: suspend () -> String) = run {
        initialize("") { _, callback ->
            applicationScope.launch(
                onError = { callback.error(it) },
                onSuccess = { callback.ok(refreshToken()) }
            )
        }
    }


}