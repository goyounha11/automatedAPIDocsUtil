package com.goyounha11.api.dto

class UserCreateRequest(
    val email: String,
    val password: String,
    val nickname: String
)

class UserCreateData(
    id: Long,
    email: String,
    password: String,
    nickname: String
) {
    val id: Long = id
    val email: String = email
    val password: String = password
    val nickname: String = nickname
}