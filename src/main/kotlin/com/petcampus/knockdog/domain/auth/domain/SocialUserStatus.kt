package com.petcampus.knockdog.domain.auth.domain

/**
 * soft-delete(BaseEntity의 deleted_at)로 표현할 수 없는 소셜 계정 연동 단계라 별도 상태로 유지한다.
 * (docs/conventions/jpa-entity.md)
 */
enum class SocialUserStatus {
    /** 회원(User)과 연결됨 — 로그인 가능 */
    LINKED,

    /** 회원과 연결되지 않음 — 회원가입/재연동 대상 */
    UNLINKED,

    /** 다른 provider가 같은 이메일로 이미 LINKED 상태 — 재연동 확인 대기 */
    PENDING,
}
