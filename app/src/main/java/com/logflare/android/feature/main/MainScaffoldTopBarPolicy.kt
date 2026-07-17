package com.logflare.android.feature.main

import com.logflare.android.ui.navigation.Route

fun shouldHideScaffoldTopBar(route: String?): Boolean {
    if (route == null) return false
    return route == Route.MyPageAddMember.path ||
        route == Route.MyPageLogout.path ||
        route.startsWith("mypage/edit-member/")
}

/** Extra top padding applied under MainScaffold content. Account-owned top bars drop it. */
fun mainNavHostExtraTopPaddingDp(route: String?): Int =
    if (shouldHideScaffoldTopBar(route)) 0 else 16

