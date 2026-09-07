package com.petcampus.knockdog.domain.kindergarten.domain

import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals

class KindergartenOperatingStatusCalculatorTest {
    private val monday = LocalDate.of(2026, 9, 7)

    private fun defaultProfile(
        weekdayOpen: String = "09:00",
        weekdayClose: String = "20:00",
        offdays: List<DayOfWeek> = emptyList(),
    ) = KindergartenBusinessHour(
        name = "DEFAULT",
        weekdayOpen = LocalTime.parse(weekdayOpen),
        weekdayClose = LocalTime.parse(weekdayClose),
        weekendOpen = LocalTime.parse("11:00"),
        weekendClose = LocalTime.parse("18:00"),
        offdays = offdays,
    )

    @Test
    fun `영업시간 내면 영업중과 종료 시각을 반환한다`() {
        val now = LocalDateTime.of(monday, LocalTime.of(14, 0))

        val status = KindergartenOperatingStatusCalculator.calculate(listOf(defaultProfile()), now)

        assertEquals("영업중", status.title)
        assertEquals("20:00에 영업 종료", status.description)
    }

    @Test
    fun `아직 오픈 전이면 영업 종료와 오늘 오픈 시각을 반환한다`() {
        val now = LocalDateTime.of(monday, LocalTime.of(7, 0))

        val status = KindergartenOperatingStatusCalculator.calculate(listOf(defaultProfile()), now)

        assertEquals("영업 종료", status.title)
        assertEquals("오늘 09:00 영업 시작", status.description)
    }

    @Test
    fun `오늘이 휴무일이면 오늘 휴무를 반환하고 다음 영업일을 안내한다`() {
        val now = LocalDateTime.of(monday, LocalTime.of(14, 0))

        val status = KindergartenOperatingStatusCalculator.calculate(listOf(defaultProfile(offdays = listOf(DayOfWeek.MONDAY))), now)

        assertEquals("오늘 휴무", status.title)
        assertEquals("내일 09:00 영업 시작", status.description)
    }

    @Test
    fun `DEFAULT 프로필이 없으면 다른 서비스 프로필로 대체하지 않고 정보 없음을 반환한다`() {
        val now = LocalDateTime.of(monday, LocalTime.of(14, 0))
        val hotelProfile =
            KindergartenBusinessHour(
                name = "HOTEL",
                weekdayOpen = LocalTime.parse("00:00"),
                weekdayClose = LocalTime.parse("23:59"),
                weekendOpen = LocalTime.parse("00:00"),
                weekendClose = LocalTime.parse("23:59"),
                offdays = emptyList(),
            )

        val status = KindergartenOperatingStatusCalculator.calculate(listOf(hotelProfile), now)

        assertEquals("오늘 휴무", status.title)
        assertEquals("영업시간 정보 없음", status.description)
    }

    @Test
    fun `프로필명이 기본이어도 DEFAULT와 동일하게 인식한다`() {
        val now = LocalDateTime.of(monday, LocalTime.of(14, 0))
        val profile = defaultProfile().copy(name = "기본")

        val status = KindergartenOperatingStatusCalculator.calculate(listOf(profile), now)

        assertEquals("영업중", status.title)
    }

    @Test
    fun `DEFAULT가 오늘 휴무면 다른 프로필의 영업시간을 빌려오지 않는다`() {
        val now = LocalDateTime.of(monday, LocalTime.of(14, 0))
        val closedDefault = defaultProfile(offdays = listOf(DayOfWeek.MONDAY))
        val hotelProfile =
            KindergartenBusinessHour(
                name = "HOTEL",
                weekdayOpen = LocalTime.parse("00:00"),
                weekdayClose = LocalTime.parse("23:59"),
                weekendOpen = LocalTime.parse("00:00"),
                weekendClose = LocalTime.parse("23:59"),
                offdays = emptyList(),
            )

        val status = KindergartenOperatingStatusCalculator.calculate(listOf(closedDefault, hotelProfile), now)

        assertEquals("오늘 휴무", status.title)
        assertEquals("내일 09:00 영업 시작", status.description)
    }

    @Test
    fun `영업시간 정보가 전혀 없으면 영업시간 정보 없음을 반환한다`() {
        val now = LocalDateTime.of(monday, LocalTime.of(14, 0))

        val status = KindergartenOperatingStatusCalculator.calculate(emptyList(), now)

        assertEquals("오늘 휴무", status.title)
        assertEquals("영업시간 정보 없음", status.description)
    }
}
