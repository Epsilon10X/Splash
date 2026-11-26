package com.splash.offline.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.splash.offline.R
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SplashAudioManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentRawRes: Int = 0
    var isPlaying: Boolean = false
        private set

    fun startForBiome(biomeId: String) {
        val rawRes = when (biomeId) {
            "costa", "fiordo", "atolon" -> R.raw.costa_waves
            else -> R.raw.bali_water
        }
        try {
            if (mediaPlayer != null && currentRawRes == rawRes) {
                if (!mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.start()
                }
                isPlaying = true
                return
            }
            stop()
            val mp = MediaPlayer.create(context.applicationContext, rawRes) ?: return
            mp.isLooping = true
            mp.setVolume(0.55f, 0.55f)
            mp.start()
            mediaPlayer = mp
            currentRawRes = rawRes
            isPlaying = true
        } catch (_: Throwable) {
            isPlaying = false
        }
    }

    fun toggle(biomeId: String): Boolean {
        if (isPlaying) {
            stop()
        } else {
            startForBiome(biomeId)
        }
        return isPlaying
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Throwable) {
        } finally {
            mediaPlayer = null
            currentRawRes = 0
            isPlaying = false
        }
    }

    fun playGentleCompletionChime() {
        Thread {
            var track: AudioTrack? = null
            try {
                val sampleRate = 44100
                val totalSec = 1.15
                val numSamples = (sampleRate * totalSec).toInt()
                val pcm = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var sample = 0.0

                    // Note 1: warm C5 + soft octave overtone
                    if (t >= 0.0) {
                        val env1 = (t / 0.025).coerceAtMost(1.0) * exp(-t * 4.4)
                        sample += (sin(2.0 * PI * 523.25 * t) * 0.78 + sin(2.0 * PI * 1046.50 * t) * 0.22) * env1
                    }
                    // Note 2: gentle G5 + soft octave overtone entering smoothly at 0.20s
                    if (t >= 0.20) {
                        val t2 = t - 0.20
                        val env2 = (t2 / 0.030).coerceAtMost(1.0) * exp(-t2 * 3.1)
                        sample += (sin(2.0 * PI * 783.99 * t2) * 0.80 + sin(2.0 * PI * 1567.98 * t2) * 0.18) * env2
                    }

                    val scaled = (sample * 0.34 * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    pcm[i] = scaled.toShort()
                }

                val bufBytes = pcm.size * 2
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(bufBytes)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((totalSec * 1000).toLong() + 120L)
            } catch (_: Throwable) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }.apply { isDaemon = true }.start()
    }
}
