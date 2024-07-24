package com.goyounha11.api

import com.goyounha11.api.dto.*
import com.goyounha11.core.reponse.ApiResult
import com.goyounha11.core.reponse.Result
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): ApiResult<Unit> {
        return Result.ok()
    }

    @GetMapping
    fun getUser(): ApiResult<PageImpl<UserCreateData>> {
        val users = MutableList(10) { i ->
            UserCreateData(
                i.toLong(),
                UserStatus.ACTIVE,
                "test$i@gmail.com",
                "1234",
                "테스터$i",
                UserAddressData("Seoul", "Gangnam", 12345),
                mutableListOf(HobbyData("soccer"), HobbyData("baseball"), HobbyData("basketball"))
            )
        }


        val pageRequest = PageRequest.of(0, 10)  // 페이지 번호와 페이지 크기
        val userPage = PageImpl(users, pageRequest, users.size.toLong())

        return Result.ok(userPage)
    }
}

