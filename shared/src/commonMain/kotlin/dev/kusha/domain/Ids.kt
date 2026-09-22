package dev.kusha.domain

import kotlin.random.Random

object Ids {
    private const val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz"
    fun next(prefix: String = ""): String {
        val sb = StringBuilder(prefix)
        repeat(16) { sb.append(ALPHABET[Random.nextInt(ALPHABET.length)]) }
        return sb.toString()
    }
}
