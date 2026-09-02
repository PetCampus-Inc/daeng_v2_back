package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import com.petcampus.knockdog.global.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "kindergarten_business_hours")
class KindergartenBusinessHourJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "kindergarten_id", nullable = false)
    val kindergartenId: Long,
    @Column(name = "name", nullable = false, length = 30)
    val name: String,
    @Column(name = "weekday_open")
    val weekdayOpen: LocalTime?,
    @Column(name = "weekday_close")
    val weekdayClose: LocalTime?,
    @Column(name = "weekend_open")
    val weekendOpen: LocalTime?,
    @Column(name = "weekend_close")
    val weekendClose: LocalTime?,
    @Convert(converter = DayOfWeekListConverter::class)
    @Column(name = "offdays")
    val offdays: List<DayOfWeek>,
    deletedAt: LocalDateTime? = null,
) : BaseEntity(deletedAt)
