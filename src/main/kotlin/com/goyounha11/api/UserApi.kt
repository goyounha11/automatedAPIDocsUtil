package com.goyounha11.api

import com.goyounha11.api.dto.*
import com.goyounha11.core.reponse.ApiResult
import com.goyounha11.core.reponse.Result
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user")
class UserApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody req: UserCreateRequest): ApiResult<UserCreateData> {
        return Result.created(
            UserCreateData(
                1L,
                UserStatus.ACTIVE,
                req.email,
                req.password,
                req.nickname,
                UserAddressData("Seoul", "Gangnam", 12345),
                mutableListOf(HobbyData("soccer"), HobbyData("baseball"), HobbyData("basketball"))
            ));
    }
}