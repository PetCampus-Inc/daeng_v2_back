package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveUserPort
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class UserPersistenceAdapter(
    private val userJpaRepository: UserJpaRepository,
) : LoadUserPort,
    SaveUserPort {
    override fun save(user: User): User = userJpaRepository.save(user.toJpaEntity()).toDomain()

    override fun findById(id: UserId): User? = userJpaRepository.findByIdOrNull(id.value)?.toDomain()

    override fun findByCode(code: UserCode): User? = userJpaRepository.findByUserCode(code.value)?.toDomain()
}
