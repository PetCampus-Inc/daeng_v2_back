package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.global.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "checklist_submissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_code", "target_id"])],
)
class ChecklistSubmissionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "user_code", nullable = false, length = 8)
    val userCode: String,
    @Column(name = "target_id", nullable = false, length = 100)
    val targetId: String,
    @Column(name = "template_version", nullable = false, length = 50)
    var templateVersion: String,
    @Convert(converter = ChecklistAnswersJsonConverter::class)
    @Column(name = "answers", nullable = false, columnDefinition = "TEXT")
    var answers: Map<String, String>,
) : BaseEntity()
