package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOptionGroup

/** 웹 응답 전용 매핑 헬퍼 — 옵션 코드를 화면 노출 목록으로 바꾼다. */
object KindergartenServiceTags {
    /** `main/{id}`의 `serviceTags` — 4개 그룹을 합친 평평한 목록. */
    fun allOf(kindergarten: Kindergarten): List<String> = KindergartenOptionGroup.entries.flatMap { group(kindergarten, it) }

    fun group(
        kindergarten: Kindergarten,
        group: KindergartenOptionGroup,
    ): List<String> = kindergarten.optionsOf(group).map { it.code }

    /** `main/{id}`의 `banner` — 레거시와 동일하게 [썸네일] + 요금표 이미지를 이어붙인다. */
    fun banner(kindergarten: Kindergarten): List<String> {
        val banner = mutableListOf<String>()
        kindergarten.thumbnailS3Key?.takeIf { it.isNotBlank() }?.let { banner.add(it) }
        banner.addAll(kindergarten.priceImages.sortedBy { it.displayOrder }.map { it.s3Key })
        return banner
    }
}
