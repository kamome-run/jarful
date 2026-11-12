package dev.jarful.sound

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedurally generated "paper crumple" and "drop" sounds (FR-4.2) so the app ships no binary assets.
 * Crumple = a burst of short filtered-noise crackles with a decaying envelope.
 */
object CrumpleSound {
    const val SAMPLE_RATE = 22_050

    fun crumple(seed: Int = 7): ShortArray {
        val rnd = Random(seed)
        val n = (SAMPLE_RATE * 0.55).toInt()
        val out = FloatArray(n)
        // crackles: ~40 short bursts at random times, denser at the start
        val bursts = 42
        repeat(bursts) { i ->
            val t0 = ((i.toFloat() / bursts) * 0.85f * n * rnd.nextFloat().coerceAtLeast(0.15f)).toInt()
            val len = (SAMPLE_RATE * (0.004 + rnd.nextDouble() * 0.014)).toInt()
            val amp = 0.35f + rnd.nextFloat() * 0.65f
            var prev = 0f
            for (k in 0 until len) {
                val idx = t0 + k
                if (idx >= n) break
                val env = exp(-6.0 * k / len).toFloat()
                // simple high-pass-ish noise (difference of white noise)
                val w = rnd.nextFloat() * 2f - 1f
                val hp = w - prev * 0.5f
                prev = w
                out[idx] += hp * env * amp
            }
        }
        // overall decay
        for (i in 0 until n) {
            val env = exp(-3.2 * i / n).toFloat()
            out[i] *= env
        }
        // "plop" into the jar at the end: short low sine
        val plopStart = (n * 0.62).toInt()
        val plopLen = (SAMPLE_RATE * 0.12).toInt()
        for (k in 0 until plopLen) {
            val idx = plopStart + k
            if (idx >= n) break
            val f = 220.0 - 120.0 * k / plopLen
            val env = exp(-8.0 * k / plopLen).toFloat()
            out[idx] += (sin(2 * PI * f * k / SAMPLE_RATE) * 0.5 * env).toFloat()
        }
        return normalize(out)
    }

    fun chime(): ShortArray {
        val n = (SAMPLE_RATE * 0.5).toInt()
        val out = FloatArray(n)
        val freqs = doubleArrayOf(880.0, 1174.66, 1318.51)
        for (i in 0 until n) {
            val env = exp(-4.0 * i / n).toFloat()
            var v = 0f
            freqs.forEachIndexed { j, f -> v += (sin(2 * PI * f * i / SAMPLE_RATE) * (0.5 - j * 0.12) * env).toFloat() }
            out[i] = v
        }
        return normalize(out)
    }

    private fun normalize(x: FloatArray): ShortArray {
        var peak = 0f
        for (v in x) if (kotlin.math.abs(v) > peak) peak = kotlin.math.abs(v)
        val g = if (peak > 0f) 0.85f / peak else 1f
        return ShortArray(x.size) { (x[it] * g * 32767f).toInt().coerceIn(-32768, 32767).toShort() }
    }
}
