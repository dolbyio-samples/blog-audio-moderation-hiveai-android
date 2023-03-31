package io.dolby.android.audiomoderationsample.features.audiorecord

import android.content.Context
import android.media.AudioFormat
import com.voxeet.VoxeetSDK
import com.voxeet.android.media.stream.LocalInputAudioSamples
import com.voxeet.sdk.logger.VoxeetLogger
import io.sentry.util.CircularFifoQueue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import javax.inject.Inject

class ObserveLocalAudioSamplesUseCase @Inject constructor() {

    companion object {

        const val bufferSizeSeconds = 10
    }

    private val logger: VoxeetLogger = VoxeetLogger(this::class.java.simpleName)

    private var bufferSize = 0
    private var circularBuffer = java.util.Collections.synchronizedCollection(CircularFifoQueue<Byte>())
    private var sampleRate = 0
    private var audioFormat: Int = 0
    private var channelCount: Short = 0

    private val _localAudioSamples = MutableSharedFlow<LocalInputAudioSamples>(replay = 1)
    private val localAudioSamples = _localAudioSamples.asSharedFlow()

    private lateinit var job: Job

    init {
        VoxeetSDK.audio().local.registerLocalInputAudioCallback {
            GlobalScope.launch {
                _localAudioSamples.emit(it)
            }
        }
    }

    fun run() {
        job = CoroutineScope(Dispatchers.IO).launch { localAudioSamples.collect { actualSamples ->
            if (sampleRate != actualSamples.sampleRate) {
                // reset the values
                sampleRate = actualSamples.sampleRate
                audioFormat = actualSamples.audioFormat
                channelCount = actualSamples.channelCount.toShort()
                when (audioFormat) {
                    AudioFormat.ENCODING_PCM_FLOAT -> bufferSize = bufferSizeSeconds * actualSamples.sampleRate * actualSamples.channelCount * 4
                    AudioFormat.ENCODING_PCM_16BIT -> bufferSize = bufferSizeSeconds * actualSamples.sampleRate * actualSamples.channelCount * 2
                    else -> throw java.lang.IllegalStateException("Audio format not supported ${audioFormat}")
                }
                circularBuffer = java.util.Collections.synchronizedCollection(CircularFifoQueue<Byte>(bufferSize))

            }

            circularBuffer.addAll(actualSamples.data.toList())
        } }
    }

    fun stop() {
        if (::job.isInitialized) {
            job.cancel()
        }
    }

    fun getWaveFile(context: Context, onSuccess: (File) -> Unit, onError: (Throwable) -> Unit) = CoroutineScope(Dispatchers.IO).launch {

        logger.d("Start creating wave file")
        val arrayCopy = circularBuffer.toTypedArray()
        val moderationRecords = File(context.filesDir, "audiomoderation")
        if (!moderationRecords.exists()) {
            moderationRecords.mkdir()
        }

        val waveFile = File(moderationRecords, "recording_audio_stream.wav")
        arrayCopy.asFile(file = waveFile, sampleRate = sampleRate , format = audioFormat, channelCount = channelCount)
        logger.d("Finished creating wave file")
        onSuccess(waveFile)
    }
}
