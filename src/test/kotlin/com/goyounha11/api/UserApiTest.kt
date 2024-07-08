package com.goyounha11.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.goyounha11.api.dto.Gender
import com.goyounha11.api.dto.UserCreateData
import com.goyounha11.api.dto.UserCreateRequest
import com.goyounha11.api.dto.UserStatus
import com.goyounha11.docs.DocsUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter

@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class UserApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext, restDocumentation: RestDocumentationContextProvider) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilter<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .build()
    }

    @Test
    fun `회원 가입`() {
        val req = UserCreateRequest("test@test.com", Gender.MALE,"1234", "테스터")

        val resultAction = mockMvc.perform(
            post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsBytes(req))
        )

        resultAction.andExpectAll(
            jsonPath("$.code").value("S000"),
            jsonPath("$.message").value("success"),
            jsonPath("$.data.id").value(1L),
            jsonPath("$.data.status").value(UserStatus.ACTIVE.name),
            jsonPath("$.data.email").value("test@test.com"),
            jsonPath("$.data.password").value("1234"),
            jsonPath("$.data.nickname").value("테스터"),
            jsonPath("$.data.hobbies[*].name").isNotEmpty,
            jsonPath("$.data.address.city").value("Seoul"),
            jsonPath("$.data.address.street").value("Gangnam"),
            jsonPath("$.data.address.zipcode").value(12345)
        )

        resultAction.andDo(
            DocsUtil.createDocs(
                "User",
                "user/create",
                "유저 회원가입 API",
                resultAction,
                UserCreateRequest::class.java,
                UserCreateData::class.java
            )
        )
    }
}