package com.petcampus.knockdog.domain.bookmark.adapter.outbound.user

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkAddress
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkAddressesPort
import org.springframework.stereotype.Component

@Component
class BookmarkAddressAdapter(
    private val loadUserPort: LoadUserPort,
) : LoadBookmarkAddressesPort {
    override fun findByUserCode(userCode: String): List<BookmarkAddress> {
        val user = loadUserPort.findByCode(UserCode(userCode)) ?: return emptyList()
        return user.addresses
            .sortedBy { it.type.name != HOME_ADDRESS_TYPE }
            .map { BookmarkAddress(type = it.type.name, lat = it.lat, lng = it.lng) }
    }

    companion object {
        private const val HOME_ADDRESS_TYPE = "HOME"
    }
}
