package dev.kusha

import dev.kusha.domain.Stats
import dev.kusha.model.AppData
import dev.kusha.model.Ticket
import dev.kusha.model.TicketState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {
    private fun done(date: String, id: String) = Ticket(id = id, date = date, category = "", title = id, state = TicketState.DONE)

    @Test
    fun streakCountsConsecutiveDays() {
        val data = AppData(tickets = listOf(done("2026-09-22", "a"), done("2026-09-21", "b"), done("2026-09-20", "c"), done("2026-09-18", "d")))
        assertEquals(3, Stats.streak(data, LocalDate(2026, 9, 22)))
        // nothing today yet: streak continues from yesterday
        assertEquals(3, Stats.streak(data, LocalDate(2026, 9, 23)))
        assertEquals(0, Stats.streak(data, LocalDate(2026, 9, 25)))
    }

    @Test
    fun loopsPerDayFillsGaps() {
        val data = AppData(tickets = listOf(done("2026-09-22", "a"), done("2026-09-22", "b")))
        val per = Stats.loopsPerDay(data, LocalDate(2026, 9, 22), 3)
        assertEquals(listOf(0, 0, 2), per.map { it.second })
        assertEquals(LocalDate(2026, 9, 20), per.first().first)
    }
}
