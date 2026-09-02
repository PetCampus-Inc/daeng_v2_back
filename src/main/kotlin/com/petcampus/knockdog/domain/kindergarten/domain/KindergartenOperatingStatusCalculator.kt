package com.petcampus.knockdog.domain.kindergarten.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** "영업중" / "영업 종료" / "오늘 휴무" + 설명 문구. 레거시 `BusinessStatus` 응답과 동일한 문구 규칙을 따른다. */
data class OperatingStatusDescription(
    val title: String,
    val description: String,
)

/**
 * 유치원의 실시간 영업 상태를 계산한다. 프로필은 `DEFAULT`를 우선 사용하고, 그게 없거나 오늘이 offday면
 * offday가 아닌 다른 프로필을 쓴다(레거시 `OperationStatusParser` 이식).
 *
 * 자정을 넘겨 이어지는 영업(예: 어제 20:00 ~ 오늘 02:00)은 이번 범위에서 지원하지 않는다 — 크롤링 데이터에
 * 그런 사례가 없었고, 재현 비용 대비 실익이 낮다고 판단했다(docs/work/KD3-413-kindergarten-static-lookup.md).
 */
object KindergartenOperatingStatusCalculator {
    private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
    private const val SEARCH_DAYS_AHEAD = 14

    /** 오늘 적용되는 영업시간 범위(없으면 null). 레거시 `operationTimes`(startTime/endTime)가 참조하는 값과 같다. */
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
        val default = profiles.firstOrNull { it.name.equals("DEFAULT", ignoreCase = true) }
        if (default != null && !default.isOffday(dayOfWeek)) return default
        return profiles.firstOrNull { !it.isOffday(dayOfWeek) }
    }

    private fun isWithin(
        time: java.time.LocalTime,
        range: KindergartenBusinessHour.TimeRange,
    ): Boolean = !time.isBefore(range.start) && time.isBefore(range.end)
}
