package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOptionGroup

object KindergartenServiceTags {
    fun allOf(kindergarten: Kindergarten): List<String> = KindergartenOptionGroup.entries.flatMap { group(kindergarten, it) }

    fun group(
        kindergarten: Kindergarten,
        group: KindergartenOptionGroup,
    ): List<String> = kindergarten.optionsOf(group).map { it.code }

    fun banner(kindergarten: Kindergarten): List<String> {
        val banner = mutableListOf<String>()
        kindergarten.thumbnailS3Key?.takeIf { it.isNotBlank() }?.let { banner.add(it) }
        banner.addAll(kindergarten.priceImages.sortedBy { it.displayOrder }.map { it.s3Key })
        return banner
    }
}
