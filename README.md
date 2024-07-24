# Spring REST Docs API specification Integration
## Gradle 
```
implementation 'io.github.goyounha11:automatedAPIDocsUtil:1.0.7
```
## How Use
### 1.your project setting epages restdocs
```https://github.com/ePages-de/restdocs-api-spec```

### 2.create controller test
- follow like this
```
    fun `join_member`() {
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
### 3.createDocs parameter list
```
    1st param : Tag
    2nd param : Identifier
    3rd param : Api description
    4th param : ResultAction for generate snippets
```
### 4.you can using gradle task
```
    gradle openapi3 generateSwaggerUIConvert
```
