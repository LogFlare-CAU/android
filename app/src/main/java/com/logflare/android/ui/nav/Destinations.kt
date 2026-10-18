package com.logflare.android.ui.nav

/**
 * Every route of the app. Builders (`projectDetail(7)`) make the route to navigate to; the
 * `*Pattern` constants are what the nav graph registers, with arguments named by the `*Arg`s.
 */
object Destinations {
    const val Login = "login"

    const val Home = "home"
    const val Logs = "logs"
    const val Projects = "projects"
    const val My = "my"

    /** The bottom-bar tabs, in bar order. */
    val Tabs = listOf(Home, Logs, Projects, My)

    /** Routes that draw edge to edge and handle window insets themselves (no scaffold padding). */
    val FullBleed = setOf(Login)

    /**
     * Preset level filter for the Logs tab, as [com.logflare.android.data.store.model.LogQuery.apiMinLevel]
     * (`"ERROR"`), or null for none. Not a route argument (that would make the tab's route a
     * pattern and a restored tab ignore it): [openLogs] writes it into the Logs entry's
     * SavedStateHandle, where the Logs screen's ViewModel observes it.
     */
    const val LogsMinLevelArg = "minLevel"

    const val ProjectIdArg = "projectId"
    const val ErrorIdArg = "errorId"
    const val MemberIdxArg = "idx"

    const val ProjectCreate = "projects/new"
    const val ProjectDetailPattern = "projects/{$ProjectIdArg}"
    const val ProjectSettingsPattern = "projects/{$ProjectIdArg}/settings"
    fun projectDetail(id: Int) = "projects/$id"
    fun projectSettings(id: Int) = "projects/$id/settings"

    const val LogDetailPattern = "logs/{$ErrorIdArg}"
    fun logDetail(id: Int) = "logs/$id"

    const val AddMember = "my/members/new"
    const val EditMemberPattern = "my/members/{$MemberIdxArg}"
    fun editMember(idx: Int) = "my/members/$idx"

    /** Set to true in My's back-stack entry by Edit member when it left after saving: My shows "Saved". */
    const val MemberSavedResult = "memberSaved"
}
