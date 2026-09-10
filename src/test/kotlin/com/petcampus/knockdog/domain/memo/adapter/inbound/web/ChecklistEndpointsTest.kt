package com.petcampus.knockdog.domain.memo.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChecklistEndpointsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var tokenPort: TokenPort

    private fun bearer(userCode: String = "A1B2C3D4") = "Bearer " + tokenPort.issueAccessToken(UserCode(userCode))

    @Test
    fun `템플릿 조회는 인증이 필요하다`() {
        mockMvc.perform(get("/api/v1/checklists/template")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `템플릿은 5섹션 13문항, 문항 type 문자열`() {
        mockMvc
            .perform(get("/api/v1/checklists/template").header("Authorization", bearer()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.template.code").value("registration"))
            .andExpect(jsonPath("$.data.sections.length()").value(5))
            .andExpect(jsonPath("$.data.sections[1].questions[3].id").value("q_max_dogs_per_day"))
            .andExpect(jsonPath("$.data.sections[1].questions[3].type").value("INTEGER"))
    }

    @Test
    fun `답변 없으면 GET은 200에 sections 빈 배열`() {
        mockMvc
            .perform(get("/api/v1/checklists/place-1").header("Authorization", bearer()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.sections").isArray)
            .andExpect(jsonPath("$.data.sections.length()").value(0))
    }

    @Test
    fun `PUT 저장 후 GET하면 정규화된 값이 템플릿 순서로 나온다`() {
        mockMvc
            .perform(
                put("/api/v1/checklists/place-1")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"answers":[{"questionId":"q_vaccine_proof_required","value":"yes"},{"questionId":"q_max_dogs_per_day","value":"30"}]}""",
                    ),
            ).andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/checklists/place-1").header("Authorization", bearer()))
            .andExpect(jsonPath("$.data.sections[0].sectionId").value("sec_register"))
            .andExpect(jsonPath("$.data.sections[0].answers[0].value").value("YES"))
            .andExpect(jsonPath("$.data.sections[0].answers[0].value").isString)
    }

    @Test
    fun `모르는 questionId면 400 MEMO_INVALID_CHECKLIST_ANSWER`() {
        mockMvc
            .perform(
                put("/api/v1/checklists/place-1")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"answers":[{"questionId":"q_bogus","value":"YES"}]}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MEMO_INVALID_CHECKLIST_ANSWER"))
    }
}
