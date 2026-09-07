package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.SaveKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class KindergartenPersistenceAdapter(
    private val kindergartenJpaRepository: KindergartenJpaRepository,
    private val categoryJpaRepository: KindergartenCategoryJpaRepository,
    private val businessHourJpaRepository: KindergartenBusinessHourJpaRepository,
    private val linkJpaRepository: KindergartenLinkJpaRepository,
    private val optionJpaRepository: KindergartenOptionJpaRepository,
    private val priceImageJpaRepository: KindergartenPriceImageJpaRepository,
    private val menuJpaRepository: KindergartenMenuJpaRepository,
) : LoadKindergartenPort,
    SaveKindergartenPort {
    @Transactional(readOnly = true)
    override fun findByNaverPlaceId(naverPlaceId: String): Kindergarten? {
        val entity = kindergartenJpaRepository.findByNaverPlaceId(naverPlaceId) ?: return null
        return assemble(entity)
    }

    @Transactional
    override fun save(kindergarten: Kindergarten): Kindergarten {
        val savedRoot = kindergartenJpaRepository.save(kindergarten.toJpaEntity())
        val kindergartenId = requireNotNull(savedRoot.id)

        categoryJpaRepository.saveAll(kindergarten.toCategoryJpaEntities(kindergartenId))
        businessHourJpaRepository.saveAll(kindergarten.toBusinessHourJpaEntities(kindergartenId))
        linkJpaRepository.saveAll(kindergarten.toLinkJpaEntities(kindergartenId))
        optionJpaRepository.saveAll(kindergarten.toOptionJpaEntities(kindergartenId))
        priceImageJpaRepository.saveAll(kindergarten.toPriceImageJpaEntities(kindergartenId))
        menuJpaRepository.saveAll(kindergarten.toMenuJpaEntities(kindergartenId))

        return assemble(savedRoot)
    }

    private fun assemble(entity: KindergartenJpaEntity): Kindergarten {
        val id = requireNotNull(entity.id)
        return entity.toDomain(
            categories = categoryJpaRepository.findAllByKindergartenId(id),
            businessHours = businessHourJpaRepository.findAllByKindergartenId(id),
            links = linkJpaRepository.findAllByKindergartenId(id),
            options = optionJpaRepository.findAllByKindergartenId(id),
            priceImages = priceImageJpaRepository.findAllByKindergartenIdOrderByDisplayOrder(id),
            menus = menuJpaRepository.findAllByKindergartenIdOrderByDisplayOrder(id),
        )
    }

    override fun existsByNaverPlaceId(naverPlaceId: String): Boolean = kindergartenJpaRepository.findByNaverPlaceId(naverPlaceId) != null

    override fun count(): Long = kindergartenJpaRepository.count()
}
