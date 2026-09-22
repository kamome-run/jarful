package dev.jarful.domain

import dev.jarful.model.AppData
import dev.jarful.model.Routine
import dev.jarful.model.Ticket
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** Generates routine tickets per weekday (FR-6). Pure and testable. */
object RoutineScheduler {

    fun routinesFor(routines: List<Routine>, date: LocalDate): List<Routine> =
        routines.filter { it.enabled && date.dayOfWeek in it.weekdays }.sortedBy { it.order }

    /**
     * Which dates should be generated when the app is opened at [now]:
     * always today; plus tomorrow once the prepare time has passed (FR-6.3).
     */
    fun datesToPrepare(now: LocalDateTime, prepareHour: Int, prepareMinute: Int): List<LocalDate> {
        val today = now.date
        val afterPrepare = now.hour > prepareHour || (now.hour == prepareHour && now.minute >= prepareMinute)
        return if (afterPrepare) listOf(today, Dates.plusDays(today, 1)) else listOf(today)
    }

    /** Returns data with routine tickets generated for [date] if not yet done. Idempotent. */
    fun generateFor(data: AppData, date: LocalDate, now: Long): AppData {
        val iso = Dates.iso(date)
        if (iso in data.routineGeneratedDates) return data
        val existingRoutineIds = data.tickets.filter { it.date == iso }.mapNotNull { it.routineId }.toSet()
        val routines = routinesFor(data.routines, date).filter { it.id !in existingRoutineIds }
        val newTickets = routines.mapIndexed { i, r ->
            Ticket(
                id = Ids.next("k"), date = iso, routineId = r.id, category = r.category, title = r.title,
                estimateMin = r.estimateMin, timeboxMin = r.timeboxMin, quotaTarget = r.quotaTarget,
                order = i - 10_000, // routines come first (FR-6.4)
            )
        }
        return data.copy(
            tickets = data.tickets + newTickets,
            routineGeneratedDates = data.routineGeneratedDates + iso,
        )
    }

    fun generateAll(data: AppData, now: LocalDateTime, nowMillis: Long): AppData {
        var d = data
        datesToPrepare(now, data.settings.prepareHour, data.settings.prepareMinute).forEach { date ->
            d = generateFor(d, date, nowMillis)
        }
        return d
    }
}
