package com.petcampus.knockdog.domain.kindergarten.application.service

import com.petcampus.knockdog.domain.kindergarten.application.KindergartenErrorCode
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensCommand
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadComparisonAddressesPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePointType
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenId
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenSource
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenStatus
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompareKindergartensServiceTest {
    private class StubLoadKindergartenPort(
        kindergartens: List<Kindergarten>,
    ) : LoadKindergartenPort {
        private val byId = kindergartens.associateBy { it.naverPlaceId }

        override fun findByNaverPlaceId(naverPlaceId: String) = byId[naverPlaceId]

        override fun findByNaverPlaceIds(naverPlaceIds: List<String>) = naverPlaceIds.mapNotNull { byId[it] }
    }

    private class StubAddressesPort(
        private val points: List<ComparisonReferencePoint>,
    ) : LoadComparisonAddressesPort {
        override fun findByUserCode(userCode: String) = points
    }

    private fun kindergarten(
        naverPlaceId: String,
        lat: Double = 37.5,
        lng: Double = 127.0,
    ) = Kindergarten.reconstitute(
        id = KindergartenId(naverPlaceId.hashCode().toLong()),
        naverPlaceId = naverPlaceId,
        name = "유치원 $naverPlaceId",
        phoneNumber = null,
        address = "서울시 강남구",
        addressDetail = null,
        lat = lat,
        lng = lng,
        thumbnailS3Key = null,
        visitorReviewCount = 0,
        blogReviewCount = 0,
        source = KindergartenSource.CRAWLED,
        status = KindergartenStatus.ACTIVE,
        categories = emptyList(),
        businessHours = emptyList(),
        links = emptyList(),
        options = emptyList(),
        priceImages = emptyList(),
        menus = emptyList(),
    )

    private fun service(
        kindergartens: List<Kindergarten> = listOf(kindergarten("A"), kindergarten("B")),
        points: List<ComparisonReferencePoint> = emptyList(),
    ) = CompareKindergartensService(StubLoadKindergartenPort(kindergartens), StubAddressesPort(points))

    @Test
    fun `비교 대상이 2곳이 아니면 COMPARISON_TARGET_COUNT다`() {
        val exception =
            assertFailsWith<BusinessException> {
                service().compare(CompareKindergartensCommand(listOf("A"), null, null, null))
            }

        assertEquals(KindergartenErrorCode.COMPARISON_TARGET_COUNT, exception.errorCode)
    }

    @Test
    fun `비교 대상이 중복되면 COMPARISON_TARGET_DUPLICATED다`() {
        val exception =
            assertFailsWith<BusinessException> {
                service().compare(CompareKindergartensCommand(listOf("A", "A"), null, null, null))
            }

        assertEquals(KindergartenErrorCode.COMPARISON_TARGET_DUPLICATED, exception.errorCode)
    }

    @Test
    fun `없는 유치원이 포함되면 RESOURCE_NOT_FOUND다`() {
        val exception =
            assertFailsWith<BusinessException> {
                service().compare(CompareKindergartensCommand(listOf("A", "Z"), null, null, null))
            }

        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `결과 유치원 순서는 요청한 ids 순서를 따른다`() {
        val result = service().compare(CompareKindergartensCommand(listOf("B", "A"), null, null, null))

        assertEquals(listOf("B", "A"), result.kindergartens.map { it.naverPlaceId })
    }

    @Test
    fun `lat lng가 오면 기준점은 OTHER 하나다`() {
        val result = service().compare(CompareKindergartensCommand(listOf("A", "B"), "USER1234", 37.4, 127.1))

        assertEquals(
            listOf(ComparisonReferencePoint(ComparisonReferencePointType.OTHER, 37.4, 127.1)),
            result.referencePoints,
        )
    }

    @Test
    fun `lat lng 없이 로그인하면 저장 주소를 기준점으로 쓰되 HOME이 먼저다`() {
        val work = ComparisonReferencePoint(ComparisonReferencePointType.OTHER, 37.1, 127.1)
        val home = ComparisonReferencePoint(ComparisonReferencePointType.HOME, 37.2, 127.2)
        val result =
            service(points = listOf(work, home))
                .compare(CompareKindergartensCommand(listOf("A", "B"), "USER1234", null, null))

        assertEquals(listOf(home, work), result.referencePoints)
    }

    @Test
    fun `비로그인이고 위치도 없으면 기준점이 비어 있다`() {
        val result = service().compare(CompareKindergartensCommand(listOf("A", "B"), null, null, null))

        assertEquals(emptyList(), result.referencePoints)
    }
}
