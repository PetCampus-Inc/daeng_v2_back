package com.petcampus.knockdog.domain.kindergarten.application.service

import com.petcampus.knockdog.domain.kindergarten.application.port.input.GetKindergartenUseCase
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * status code 참고: 레거시 `KindergartenNotFoundException`은 전용 핸들러가 없어 500으로 응답한다
 * (`GlobalControllerAdvice`의 `Exception.class` catch-all). 그 상태로 계약을 유지하지 않고
 * `CommonErrorCode.RESOURCE_NOT_FOUND`(404)로 교정한다 — 로컬 응답 대조 시 이 차이를 반드시 기록하고
 * 프론트가 이 케이스를 실제로 분기하는지 확인한다(docs/work/KD3-413-kindergarten-static-lookup.md).
 */
@Service
class GetKindergartenService(
    private val loadKindergartenPort: LoadKindergartenPort,
) : GetKindergartenUseCase {
    @Transactional(readOnly = true)
    override fun getByNaverPlaceId(naverPlaceId: String): Kindergarten =
        loadKindergartenPort.findByNaverPlaceId(naverPlaceId) ?: throw BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
}
