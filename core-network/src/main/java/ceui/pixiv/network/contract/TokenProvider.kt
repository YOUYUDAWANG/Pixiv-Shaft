package ceui.pixiv.network.contract

/**
 * Contract for retrieving and refreshing OAuth credentials without coupling
 * the network library to Android storage or Activity/Fragment lifecycle.
 */
interface TokenProvider {
    /**
     * Returns the current Bearer token or null/empty if not logged in.
     */
    fun getBearerToken(): String?

    /**
     * Refreshes the access token synchronously on the calling worker thread.
     * Returns the newly obtained access token, or null if refresh failed.
     */
    fun refreshTokenBlocking(staleToken: String): String?

    /**
     * Invoked when a refresh token is permanently revoked or rejected by Pixiv.
     */
    fun onSessionRevoked()
}
