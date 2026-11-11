package com.noorpro.app.data.media.edit

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 16-bit mono WAV read / write — the voiceover format (no encoder needed, Transformer reads WAV). */
object Wav {
    fun write(f: File, pcm: ShortArray, rate: Int = VoiceMix.RATE) {
        val data = pcm.size * 2
        val bb = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + data); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1); bb.putInt(rate); bb.putInt(rate * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(data)
        pcm.forEach { bb.putShort(it) }
        f.parentFile?.mkdirs(); f.writeBytes(bb.array())
    }

    /** Reads the data chunk of a 16-bit mono WAV written by [write] / [VoiceRecorder]. */
    fun read(f: File): ShortArray {
        val bytes = f.readBytes(); if (bytes.size < 44) return ShortArray(0)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var p = 12
        while (p + 8 <= bytes.size) {
            val id = String(bytes, p, 4); val len = bb.getInt(p + 4)
            if (id == "data") { val n = minOf(len, bytes.size - p - 8) / 2; return ShortArray(n) { bb.getShort(p + 8 + it * 2) } }
            p += 8 + len
        }
        return ShortArray(0)
    }

    fun durationMs(f: File, rate: Int = VoiceMix.RATE): Long = ((f.length() - 44).coerceAtLeast(0) / 2) * 1000L / rate
}

/**
 * Records one voiceover take with AudioRecord (RECORD_AUDIO is already in the manifest and asked at
 * runtime by the caller). Writes PCM straight into a WAV file and patches the header on stop.
 */
class VoiceRecorder(private val out: File, private val rate: Int = VoiceMix.RATE) {
    @Volatile private var running = false
    private var thread: Thread? = null
    @Volatile var level = 0f; private set
    @Volatile var recordedMs = 0L; private set

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return false
        val rec = runCatching { AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 2) }.getOrNull() ?: return false
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return false }
        out.parentFile?.mkdirs()
        val raf = RandomAccessFile(out, "rw").apply { setLength(0); write(ByteArray(44)) }
        running = true
        rec.startRecording()
        thread = Thread {
            val buf = ShortArray(min); val bytes = ByteBuffer.allocate(min * 2).order(ByteOrder.LITTLE_ENDIAN)
            var total = 0L
            try {
                while (running) {
                    val n = rec.read(buf, 0, buf.size); if (n <= 0) continue
                    bytes.clear(); var peak = 0
                    for (i in 0 until n) { bytes.putShort(buf[i]); peak = maxOf(peak, kotlin.math.abs(buf[i].toInt())) }
                    raf.write(bytes.array(), 0, n * 2); total += n
                    level = peak / 32768f; recordedMs = total * 1000 / rate
                }
            } finally {
                runCatching { rec.stop() }; rec.release()
                val data = (total * 2).toInt()
                val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                h.put("RIFF".toByteArray()); h.putInt(36 + data); h.put("WAVE".toByteArray()); h.put("fmt ".toByteArray()); h.putInt(16)
                h.putShort(1); h.putShort(1); h.putInt(rate); h.putInt(rate * 2); h.putShort(2); h.putShort(16); h.put("data".toByteArray()); h.putInt(data)
                raf.seek(0); raf.write(h.array()); raf.close()
            }
        }.apply { name = "noor-voiceover"; start() }
        return true
    }

    /** Stops and returns the take's length in ms. */
    fun stop(): Long { running = false; thread?.join(1500); return Wav.durationMs(out, rate) }
}
