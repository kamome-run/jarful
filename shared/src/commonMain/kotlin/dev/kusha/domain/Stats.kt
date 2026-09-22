package dev.kusha.domain

import dev.kusha.model.AppData
import dev.kusha.model.Routine
import dev.kusha.model.TicketState
import kotlinx.datetime.LocalDate

data class RoutineRate(val routine: Routine, val done: Int, val scheduled: Int) {
    val ratio: Float get() = if (scheduled == 0) 0f else done.toFloat() / scheduled
}

object Stats {

    /** Loops (completed tickets) per date, over the last [days] days ending [today] (FR-11.1). */
    fun loopsPerDay(data: AppData, today: LocalDate, days: Int): List<Pair<LocalDate, Int>> {
        val counts = data.tickets.filter { it.state == TicketState.DONE }.groupingBy { it.date }.eachCount()
        return (days - 1 downTo 0).map { back ->
            val d = Dates.minusDays(today, back)
            d to (counts[Dates.iso(d)] ?: 0)
        }
    }

    /** Consecutive days (ending today or yesterday) with at least one loop (FR-11.2). */
    fun streak(data: AppData, today: LocalDate): Int {
        val done = data.tickets.filter { it.state == TicketState.DONE }.map { it.date }.toSet()
        var d = if (Dates.iso(today) in done) today else Dates.minusDays(today, 1)
        var n = 0
        while (Dates.iso(d) in done) { n++; d = Dates.minusDays(d, 1) }
        return n
    }

    /** Routine completion over the last [days] days (FR-11.3). */
    fun routineRates(data: AppData, today: LocalDate, days: Int): List<RoutineRate> {
        val from = Dates.minusDays(today, days - 1)
        return data.routines.map { r ->
            val tickets = data.tickets.filter { it.routineId == r.id && Dates.parse(it.date) >= from && Dates.parse(it.date) <= today }
            RoutineRate(r, tickets.count { it.state == TicketState.DONE }, tickets.size)
        }
    }

    fun loopsOn(data: AppData, iso: String): Int = data.tickets.count { it.date == iso && it.state == TicketState.DONE }
}
