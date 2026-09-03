package com.petcampus.knockdog.domain.breed.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BreedJpaRepository : JpaRepository<BreedJpaEntity, Long> {
    fun findAllByOrderByDisplayOrderAsc(): List<BreedJpaEntity>

    @Query(
        value = """
        select breed from BreedJpaEntity breed
        where replace(breed.nameKo, ' ', '') like concat('%', :query, '%') escape '\'
           or replace(breed.alias, ' ', '') like concat('%', :query, '%') escape '\'
        order by case
                     when replace(breed.nameKo, ' ', '') like concat(:query, '%') escape '\'
                       or replace(breed.alias, ' ', '') like concat(:query, '%') escape '\' then 0
                     else 1
                 end,
                 breed.nameKo asc
        """,
    )
    fun search(
        @Param("query") query: String,
    ): List<BreedJpaEntity>
}
