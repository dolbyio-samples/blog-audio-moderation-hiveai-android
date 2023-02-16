package io.dolby.android.audiomoderationsample.features.audiorecord

import android.media.AudioFormat
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


fun Array<Byte>.asFile(file: File, sampleRate: Int, format: Int, channelCount: Short): File {
    var wavOut: FileOutputStream?

    val dataBytes = this.toByteArray()
    try {
        wavOut = FileOutputStream(file)
    } catch (e: FileNotFoundException) {
        throw e
    }
    try {
        writeWavHeader(wavOut, channelCount, sampleRate, format, dataBytes.size)
    } catch (e: Throwable) {
    }

    try {
        wavOut.write(dataBytes)
    } catch (e: Throwable) {
    }

    try {
        wavOut.close()
    } catch (e: IOException) {
    } finally {
        wavOut = null
    }

    return file
}

@Throws(IOException::class)
private fun writeWavHeader(out: OutputStream, channels: Short, sampleRate: Int, encoding: Int, dataSize: Int) {

    // Convert the multi-byte integers to raw bytes in little endian format as required by the spec

    val (bitDepth, format) = when (encoding) {
        AudioFormat.ENCODING_PCM_8BIT -> Pair<Short, Short>(8, 1)
        AudioFormat.ENCODING_PCM_16BIT -> Pair<Short, Short>(16, 1)
        AudioFormat.ENCODING_PCM_FLOAT -> Pair<Short, Short>(32, 3)
        else -> throw IllegalArgumentException("Unacceptable encoding")
    }
    val audioFormat = ByteBuffer
        .allocate(2)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putShort(format)
        .array()

    val littleBytes = ByteBuffer
        .allocate(14)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putShort(channels)
        .putInt(sampleRate)
        .putInt(sampleRate * channels * (bitDepth / 8))
        .putShort((channels * (bitDepth / 8)).toShort())
        .putShort(bitDepth)
        .array()

    val chunkSizeBytes = ByteBuffer
        .allocate(4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(dataSize + 44 - 8)
        .array()
    val dataSizeBytes = ByteBuffer
        .allocate(4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(dataSize)
        .array()

    // Not necessarily the best, but it's very easy to visualize this way
    out.write(
        byteArrayOf( // RIFF header
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(), // ChunkID
            chunkSizeBytes[0], chunkSizeBytes[1], chunkSizeBytes[2], chunkSizeBytes[3], // ChunkSize
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(), // Format
            // fmt subchunk
            'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(), // Subchunk1ID
            16, 0, 0, 0, // Subchunk1Size
            audioFormat[0], audioFormat[1], // AudioFormat
            littleBytes[0], littleBytes[1], // NumChannels
            littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
            littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
            littleBytes[10], littleBytes[11], // BlockAlign
            littleBytes[12], littleBytes[13], // BitsPerSample
            // data subchunk
            'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(), // Subchunk2ID
            dataSizeBytes[0], dataSizeBytes[1], dataSizeBytes[2], dataSizeBytes[3] // DataSize
        )
    )
}