package io.dolby.android.audiomoderationsample.features.conference

import com.voxeet.VoxeetSDK
import com.voxeet.promise.solve.ThenPromise
import com.voxeet.promise.solve.ThenVoid
import com.voxeet.sdk.json.internal.ParamsHolder
import com.voxeet.sdk.logger.VoxeetLogger
import com.voxeet.sdk.media.constraints.Constraints
import com.voxeet.sdk.media.video.VideoCodecs
import com.voxeet.sdk.models.Conference
import com.voxeet.sdk.services.builders.ConferenceCreateOptions
import com.voxeet.sdk.services.builders.ConferenceJoinOptions
import com.voxeet.sdk.services.conference.spatialisation.SpatialAudioStyle
import javax.inject.Inject

class JoinConferenceUseCase @Inject constructor(){

    private val logger: VoxeetLogger = VoxeetLogger(this::class.java.simpleName)

    fun run(conferenceAlias: String, onSuccess: (Conference) -> Unit, onError: (Throwable) -> Unit) = run {

        val params = ParamsHolder().apply {
            setVideoCodec(VideoCodecs.H264.codec)
            setSimulcast(false)
            setLiveRecording(false)
            setSpatialAudioStyle(SpatialAudioStyle.DISABLED)
            setAudioOnly(true)
            setDolbyVoice(true)
        }

        val builder = ConferenceCreateOptions.Builder()
            .setConferenceAlias(conferenceAlias)
            .setParamsHolder(params)

        logger.i("Create conference : " + conferenceAlias)
        VoxeetSDK.conference().create(builder.build()).then(ThenPromise { conference ->
            VoxeetSDK.conference().ConferenceConfigurations.isDefaultOnSpeaker = true

            val constraints = Constraints(true, false)

            val joinOptions = ConferenceJoinOptions.Builder(conference)
                .setConstraints(constraints)
                .setSpatialAudio(false)
                .build()
            logger.i("Join conference alias: " + conference.alias.toString())
            VoxeetSDK.conference().join(joinOptions)
        }).then(ThenVoid { conference ->
            logger.i("Joined conference alias: " + conference.alias.toString())
            onSuccess(conference)
        }).error { error_in: Throwable? ->
            logger.e("Could not create conference" +  (error_in?.localizedMessage ?: "Null"));
            onError
        }
    }
}