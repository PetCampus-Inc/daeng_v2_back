package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.input.AgreeToTermsCommand
import com.petcampus.knockdog.domain.auth.application.port.input.AgreeToTermsUseCase
import com.petcampus.knockdog.domain.auth.application.port.input.GetAgreementStatusUseCase
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserAgreementPort
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveUserAgreementPort
import com.petcampus.knockdog.domain.auth.domain.AgreementTermType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAgreement
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserAgreementService(
    private val loadUserPort: LoadUserPort,
    private val loadUserAgreementPort: LoadUserAgreementPort,
    private val saveUserAgreementPort: SaveUserAgreementPort,
) : AgreeToTermsUseCase,
    GetAgreementStatusUseCase {
    @Transactional
    override fun agree(command: AgreeToTermsCommand) {
        val submitted = command.agreedTerms.toSet()
        if (!submitted.containsAll(AgreementTermType.REQUIRED)) {
            throw BusinessException(AuthErrorCode.REQUIRED_AGREEMENT_NOT_COMPLETED)
        }

        val userId = requireUserId(command.userCode)

        // 레거시는 insertIgnoringDuplicateKey로 중복을 흘려보낸다. 여기서는 이미 있는 약관을 빼고 넣어
        // (user_id, term_type) unique를 건드리지 않는다 — 재제출해도 최초 동의 시각이 보존된다.
        val alreadyAgreed = loadUserAgreementPort.findTermTypesByUserId(userId)
        val agreedAt = LocalDateTime.now()
        val newAgreements =
            submitted
                .filterNot { it in alreadyAgreed }
                .map { UserAgreement.create(userId, it, agreedAt) }

        if (newAgreements.isNotEmpty()) {
            saveUserAgreementPort.saveAll(newAgreements)
        }
    }

    @Transactional(readOnly = true)
    override fun hasAgreedRequiredTerms(userCode: UserCode): Boolean {
        val userId = requireUserId(userCode)
        return loadUserAgreementPort.findTermTypesByUserId(userId).containsAll(AgreementTermType.REQUIRED)
    }

    private fun requireUserId(userCode: UserCode): UserId {
        val user: User =
            loadUserPort.findByCode(userCode)
                ?: throw BusinessException(AuthErrorCode.NOT_FOUND_USER)
        return requireNotNull(user.id) { "저장되지 않은 User입니다." }
    }
}
