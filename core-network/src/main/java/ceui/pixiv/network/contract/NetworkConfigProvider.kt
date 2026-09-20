package ceui.pixiv.network.contract

/**
 * Contract providing network configuration (direct connect, proxies, DoH settings).
 */
interface NetworkConfigProvider {
    /**
     * Whether direct connect (SNI bypass / DNS override) is enabled.
     */
    val isDirectConnect: Boolean
        get() = false

    /**
     * Whether custom App API reverse proxy is enabled.
     */
    val isUseAppApiProxy: Boolean
        get() = false

    /**
     * URL prefix for App API proxy (e.g. "https://api.pxve.com/").
     */
    val appApiProxyUrl: String
        get() = ""

    /**
     * Whether DoH (DNS over HTTPS) is enabled.
     */
    val isUseSecureDns: Boolean
        get() = false

    /**
     * High-level connection mode.
     */
    val networkMode: NetworkMode
        get() = if (isDirectConnect) NetworkMode.DirectCompat else NetworkMode.Standard
}
