package com.logflare.android.data.store.model

import com.example.logflare.core.model.UserDTO

data class Member(val idx: Int, val username: String, val permission: Int)

internal fun UserDTO.toMember() = Member(idx = idx, username = username, permission = permission)
