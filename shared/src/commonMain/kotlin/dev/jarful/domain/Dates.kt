package dev.jarful.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus

object Dates {
    fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    fun today(): LocalDate = now().date
    fun toLocalDateTime(epochMillis: Long): LocalDateTime =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun iso(date: LocalDate): String = date.toString()
    fun parse(iso: String): LocalDate = LocalDate.parse(iso)
    fun plusDays(date: LocalDate, days: Int): LocalDate = date.plus(DatePeriod(days = days))
    fun minusDays(date: LocalDate, days: Int): LocalDate = date.minus(DatePeriod(days = days))
    fun dayOfWeek(iso: String): DayOfWeek = parse(iso).dayOfWeek
}
