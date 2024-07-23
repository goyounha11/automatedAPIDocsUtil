package com.goyounha11.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class UserCreateRequest(
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val gender: Gender,
    @field:NotBlank
    val password: String,
    @field:NotBlank
    val nickname: String
)

class UserCreateData(
    id: Long,
    status: UserStatus,
    email: String,
    password: String,
    nickname: String,
    address: UserAddressData,
    hobbies: List<HobbyData>
) {
    @field:NotNull
    val id: Long = id
    @field:NotNull
    var status: UserStatus = UserStatus.ACTIVE
    @field:NotBlank
    val email: String = email
    @field:NotBlank
    val password: String = password
    @field:NotBlank
    val nickname: String = nickname
    val address: UserAddressData = address
    val hobbies: List<HobbyData> = hobbies
}

class HobbyData(
    @field:NotBlank
    val name: String,
)

class UserAddressData(
    @field:NotBlank
    val city: String,
    @field:NotBlank
    val street: String,
    @field:NotBlank
    val zipcode: Int
)


enum class UserStatus {
    ACTIVE, INACTIVE
}

enum class Gender(description: String) {
    MALE("남자"), FEMALE("여자")
}