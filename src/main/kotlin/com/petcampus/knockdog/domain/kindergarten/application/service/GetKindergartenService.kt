package com.petcampus.knockdog.domain.kindergarten.application.service

import com.petcampus.knockdog.domain.kindergarten.application.port.input.GetKindergartenUseCase
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetKindergartenService(
    private val loadKindergartenPort: LoadKindergartenPort,
) : GetKindergartenUseCase {
    @Transactional(readOnly = true)
    override fun getByNaverPlaceId(naverPlaceId: String): Kindergarten =
        loadKindergartenPort.findByNaverPlaceId(naverPlaceId) ?: throw BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
}
