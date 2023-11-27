package com.goyounha11.api

import com.goyounha11.api.dto.UserCreateData
import com.goyounha11.api.dto.UserCreateRequest
import com.goyounha11.core.reponse.ApiResult
import com.goyounha11.core.reponse.Result
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody req: UserCreateRequest): ApiResult<UserCreateData> {
        return Result.created(UserCreateData(1L, req.email, req.password, req.nickname));
    }
}