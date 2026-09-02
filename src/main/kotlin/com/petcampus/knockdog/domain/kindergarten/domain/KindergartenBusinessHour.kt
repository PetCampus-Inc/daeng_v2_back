package com.petcampus.knockdog.domain.kindergarten.domain

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * 영업시간 프로필 하나. 유치원 하나가 이름별로 여러 프로필(DEFAULT/KINDERGARTEN/HOTEL 등)을 가질 수 있다 —
 * 실제로 나뉘어 관리되는 사례가 있어 단일 프로필로 합치지 않는다.
 * `name`은 크롤링 값을 그대로 담는 열린 문자열이다(우리가 닫은 분류가 아니다).
 */
data class KindergartenBusinessHour(
    val name: String,
    val weekdayOpen: LocalTime?,
    val weekdayClose: LocalTime?,
    val weekendOpen: LocalTime?,
    val weekendClose: LocalTime?,
    val offdays: List<DayOfWeek>,
) {
    fun isOffday(dayOfWeek: DayOfWeek): Boolean = dayOfWeek in offdays

    /** 주어진 요일의 영업시간 범위. offday거나 해당 요일 정보가 없으면 null. */
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
