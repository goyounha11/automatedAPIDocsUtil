# Spring REST Docs API specification Integration

## How Use

- create controller test
- follow like this
```
    fun `회원 가입`() {
        val req = UserCreateRequest("test@test.com", "1234", "테스터")

        val resultAction = mockMvc.perform(
            post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsBytes(req))
        )

        resultAction.andExpectAll(
            status().isCreated,
            jsonPath("$.code").value("S000"),
            jsonPath("$.message").value("success"),
            jsonPath("$.data.id").value(1L),
            jsonPath("$.data.email").value("test@test.com"),
            jsonPath("$.data.password").value("1234"),
            jsonPath("$.data.nickname").value("테스터")
        )

        resultAction.andDo(
            DocsUtil.createDocs(
                "User",
                "user/create",
                "유저 회원가입 API",
                resultAction
            )
        )
    }
```
- createDocs parameter list
```
    1st param : Tag
    2nd param : Identifier
    3rd param : Api description
    4rd param : ResultAction for generate snippets
```
- you can using gradle task
```
    gradle generateSwaggerUIConvert -Pspring.profiles.active=${profile}
```
- if you enter the profile, run this 
```
fun getServerUrl(profile: String): String {
    return when (profile) {
        "local" -> "http://localhost:8080"
        "dev" -> "${some dev url}"
        else -> "http://localhost:8000"
    }
}
```