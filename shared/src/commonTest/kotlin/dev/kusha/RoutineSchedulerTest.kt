package dev.kusha

import dev.kusha.domain.RoutineScheduler
import dev.kusha.model.AppData
import dev.kusha.model.Routine
import dev.kusha.model.Settings
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutineSchedulerTest {
    private val tuesdayOnly = Routine(id = "r1", title = "Gym", category = "Health", weekdays = setOf(DayOfWeek.TUESDAY))
    private val daily = Routine(id = "r2", title = "Coffee", category = "Morning", order = 0)
    private val quota = Routine(id = "r3", title = "Emails", category = "Work", quotaTarget = 10, order = 1)

    @Test
    fun onlyMatchingWeekdayGetsTicket() { // AC-4
        val data = AppData(routines = listOf(tuesdayOnly, daily))
        val monday = LocalDate(2026, 9, 21) // Monday
        val tuesday = LocalDate(2026, 9, 22)
        val mon = RoutineScheduler.generateFor(data, monday, 0)
        assertEquals(listOf("Coffee"), mon.tickets.filter { it.date == "2026-09-21" }.map { it.title })
        val tue = RoutineScheduler.generateFor(mon, tuesday, 0)
        assertEquals(setOf("Coffee", "Gym"), tue.tickets.filter { it.date == "2026-09-22" }.map { it.title }.toSet())
    }

    @Test
    fun generationIsIdempotent() {
        val data = AppData(routines = listOf(daily))
        val d = LocalDate(2026, 9, 21)
        val once = RoutineScheduler.generateFor(data, d, 0)
        val twice = RoutineScheduler.generateFor(once, d, 0)
        assertEquals(1, twice.tickets.size)
    }

    @Test
    fun prepareTimeAddsTomorrow() { // AC-4 second half
        val before = LocalDateTime(2026, 9, 21, 20, 59)
        val after = LocalDateTime(2026, 9, 21, 21, 0)
        assertEquals(listOf(LocalDate(2026, 9, 21)), RoutineScheduler.datesToPrepare(before, 21, 0))
        assertEquals(listOf(LocalDate(2026, 9, 21), LocalDate(2026, 9, 22)), RoutineScheduler.datesToPrepare(after, 21, 0))
        val all = RoutineScheduler.generateAll(AppData(routines = listOf(daily, tuesdayOnly), settings = Settings(prepareHour = 21)), after, 0)
        assertTrue(all.tickets.any { it.date == "2026-09-22" && it.title == "Gym" })
    }

    @Test
    fun routinesKeepOrderAndComeFirst() {
        val data = AppData(routines = listOf(quota, daily))
        val g = RoutineScheduler.generateFor(data, LocalDate(2026, 9, 21), 0)
        val titles = g.tickets.sortedBy { it.order }.map { it.title }
        assertEquals(listOf("Coffee", "Emails"), titles)
        assertTrue(g.tickets.all { it.order < 0 })
        assertEquals(10, g.tickets.first { it.title == "Emails" }.quotaTarget)
    }
}
