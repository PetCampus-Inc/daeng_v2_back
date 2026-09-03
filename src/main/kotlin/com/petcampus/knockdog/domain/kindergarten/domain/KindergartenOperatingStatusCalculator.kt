package com.petcampus.knockdog.domain.kindergarten.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class OperatingStatusDescription(
    val title: String,
    val description: String,
)

object KindergartenOperatingStatusCalculator {
    private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
    private const val SEARCH_DAYS_AHEAD = 14
    private val DEFAULT_PROFILE_ALIASES = setOf("default", "기본")

    fun todayRange(
        profiles: List<KindergartenBusinessHour>,
        today: java.time.LocalDate,
    ): KindergartenBusinessHour.TimeRange? = choose(profiles, today.dayOfWeek)?.rangeFor(today.dayOfWeek)

    fun calculate(
        profiles: List<KindergartenBusinessHour>,
        now: LocalDateTime,
    ): OperatingStatusDescription {
        val today = now.toLocalDate()
        val todayRange = todayRange(profiles, today)

        if (todayRange != null && isWithin(now.toLocalTime(), todayRange)) {
            return OperatingStatusDescription("영업중", "${TIME_FMT.format(todayRange.end)}에 영업 종료")
        }

        if (todayRange != null && now.toLocalTime().isBefore(todayRange.start)) {
            return OperatingStatusDescription("영업 종료", "오늘 ${TIME_FMT.format(todayRange.start)} 영업 시작")
        }

        val title = if (todayRange == null) "오늘 휴무" else "영업 종료"

        for (daysAhead in 1..SEARCH_DAYS_AHEAD) {
            val futureDate = today.plusDays(daysAhead.toLong())
            val futureChosen = choose(profiles, futureDate.dayOfWeek)
            val futureRange = futureChosen?.rangeFor(futureDate.dayOfWeek) ?: continue

            val whenText =
                if (daysAhead == 1) {
                    "내일"
                } else {
                    futureDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN) + "요일"
                }
            return OperatingStatusDescription(title, "$whenText ${TIME_FMT.format(futureRange.start)} 영업 시작")
        }

        return OperatingStatusDescription(title, "영업시간 정보 없음")
    }

    private fun choose(
        profiles: List<KindergartenBusinessHour>,
        dayOfWeek: java.time.DayOfWeek,
    ): KindergartenBusinessHour? {
        val default = profiles.firstOrNull { it.name.trim().lowercase() in DEFAULT_PROFILE_ALIASES } ?: return null
        return default.takeIf { !it.isOffday(dayOfWeek) }
    }

    private fun isWithin(
        time: java.time.LocalTime,
        range: KindergartenBusinessHour.TimeRange,
    ): Boolean = !time.isBefore(range.start) && time.isBefore(range.end)
}
