package com.petcampus.knockdog.domain.kindergarten.domain

import java.time.DayOfWeek
import java.time.LocalTime

data class KindergartenBusinessHour(
    val name: String,
    val weekdayOpen: LocalTime?,
    val weekdayClose: LocalTime?,
    val weekendOpen: LocalTime?,
    val weekendClose: LocalTime?,
    val offdays: List<DayOfWeek>,
) {
    fun isOffday(dayOfWeek: DayOfWeek): Boolean = dayOfWeek in offdays

    fun rangeFor(dayOfWeek: DayOfWeek): TimeRange? {
        if (isOffday(dayOfWeek)) return null
        val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
        val open = if (isWeekend) weekendOpen else weekdayOpen
        val close = if (isWeekend) weekendClose else weekdayClose
        return if (open != null && close != null) TimeRange(open, close) else null
    }

    data class TimeRange(
        val start: LocalTime,
        val end: LocalTime,
    )
}
