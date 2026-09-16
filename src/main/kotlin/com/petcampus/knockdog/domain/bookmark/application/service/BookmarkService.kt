package com.petcampus.knockdog.domain.bookmark.application.service

import com.petcampus.knockdog.domain.bookmark.application.BookmarkErrorCode
import com.petcampus.knockdog.domain.bookmark.application.port.input.BookmarkDistanceView
import com.petcampus.knockdog.domain.bookmark.application.port.input.BookmarkKindergartenView
import com.petcampus.knockdog.domain.bookmark.application.port.input.BookmarkView
import com.petcampus.knockdog.domain.bookmark.application.port.input.CreateBookmarkUseCase
import com.petcampus.knockdog.domain.bookmark.application.port.input.DeleteBookmarkUseCase
import com.petcampus.knockdog.domain.bookmark.application.port.input.GetBookmarksUseCase
import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkAddress
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkAddressesPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkKindergartensPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkMemoSummariesPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.SaveBookmarkPort
import com.petcampus.knockdog.domain.bookmark.domain.Bookmark
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenDistanceCalculator
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Service
class BookmarkService(
    private val saveBookmarkPort: SaveBookmarkPort,
    private val loadBookmarkPort: LoadBookmarkPort,
    private val loadBookmarkKindergartensPort: LoadBookmarkKindergartensPort,
    private val loadBookmarkMemoSummariesPort: LoadBookmarkMemoSummariesPort,
    private val loadBookmarkAddressesPort: LoadBookmarkAddressesPort,
) : CreateBookmarkUseCase,
    DeleteBookmarkUseCase,
    GetBookmarksUseCase {
    @Transactional
    override fun create(
        userCode: String,
        kindergartenId: String,
    ) {
        val kindergarten =
            loadBookmarkKindergartensPort.findById(kindergartenId)
                ?: throw BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
        if (kindergarten.closed) {
            throw BusinessException(BookmarkErrorCode.CLOSED_SCHOOL)
        }
        saveBookmarkPort.createIfAbsent(Bookmark.create(userCode, kindergartenId))
    }

    @Transactional
    override fun delete(
        userCode: String,
        kindergartenId: String,
    ) {
        if (!saveBookmarkPort.delete(userCode, kindergartenId)) {
            throw BusinessException(BookmarkErrorCode.NOT_FOUND)
        }
    }

    @Transactional(readOnly = true)
    override fun list(userCode: String): List<BookmarkView> {
        val bookmarks = loadBookmarkPort.findAllByUserCode(userCode)
        val kindergartensById =
            loadBookmarkKindergartensPort
                .findByIds(bookmarks.map { it.kindergartenId })
                .associateBy { it.id }
        val memoDatesByKindergartenId =
            loadBookmarkMemoSummariesPort
                .findByUserCode(userCode)
                .associate { it.kindergartenId to it.memoDate }
        val addresses = loadBookmarkAddressesPort.findByUserCode(userCode)

        return bookmarks.mapNotNull { bookmark ->
            val kindergarten = kindergartensById[bookmark.kindergartenId] ?: return@mapNotNull null
            BookmarkView(
                kindergarten =
                    BookmarkKindergartenView(
                        id = kindergarten.id,
                        name = kindergarten.name,
                        thumbnailS3Key = kindergarten.thumbnailS3Key,
                        categories = kindergarten.categories,
                        location = kindergarten.location,
                        price = kindergarten.price,
                        reviewCount = kindergarten.reviewCount,
                    ),
                memoDate = memoDatesByKindergartenId[kindergarten.id],
                distances = distancesOf(kindergarten.lat, kindergarten.lng, addresses),
            )
        }
    }

    private fun distancesOf(
        kindergartenLat: Double?,
        kindergartenLng: Double?,
        addresses: List<BookmarkAddress>,
    ): List<BookmarkDistanceView> {
        if (kindergartenLat == null || kindergartenLng == null) return emptyList()
        return addresses.map { address ->
            BookmarkDistanceView(
                referencePoint = address.type,
                distance =
                    String.format(
                        Locale.KOREA,
                        "%.1fkm",
                        KindergartenDistanceCalculator.calculateKm(address.lat, address.lng, kindergartenLat, kindergartenLng),
                    ),
            )
        }
    }
}
