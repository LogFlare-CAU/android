package com.logflare.android.push

/** [PushRegistrationStore] kept in memory; share one instance to simulate a process restart. */
class InMemoryPushRegistrationStore : PushRegistrationStore {
    @Volatile
    private var value: PushRegistration? = null

    override suspend fun get(): PushRegistration? = value

    override suspend fun set(registration: PushRegistration?) {
        value = registration
    }
}
