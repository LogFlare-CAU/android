package com.logflare.android.feature.main

import com.example.logflare.core.designsystem.AppLayoutRoles
import com.logflare.android.ui.navigation.Route

fun shouldHideScaffoldTopBar(route: String?): Boolean {
    if (route == null) return false
    return route == Route.MyPageAddMember.path ||
        route == Route.MyPageLogout.path ||
        route.startsWith("mypage/edit-member/")
}

/** Non-composable helper that mirrors the default role used by MainScaffold content. */
fun mainNavHostExtraTopPaddingDp(route: String?): Int =
    if (shouldHideScaffoldTopBar(route)) 0 else AppLayoutRoles().screenPadding.value.toInt()

