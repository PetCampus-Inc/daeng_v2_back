package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.SocialUser

interface SaveSocialUserPort {
    fun save(socialUser: SocialUser): SocialUser
}
