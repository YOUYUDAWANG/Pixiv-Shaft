package ceui.lisa.http;

import java.io.IOException;

import ceui.pixiv.network.BuildConfig;
import ceui.pixiv.network.contract.NetworkConfigProvider;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * App API 代理拦截器（PxveAPI 风格）。
 */
public class AppApiProxyInterceptor implements Interceptor {

    private static final String APP_API_HOST = "app-api.pixiv.net";
    private static final String OAUTH_HOST = "oauth.secure.pixiv.net";

    private final NetworkConfigProvider configProvider;

    public AppApiProxyInterceptor() {
        this(null);
    }

    public AppApiProxyInterceptor(NetworkConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request request = chain.request();
        final HttpUrl url = request.url();

        final boolean enabled = configProvider != null && configProvider.isUseAppApiProxy();
        if (!enabled) return chain.proceed(request);

        final String proxy = configProvider.getAppApiProxyUrl();
        if (!APP_API_HOST.equals(url.host()) && !OAUTH_HOST.equals(url.host())) {
            return chain.proceed(request);
        }

        final HttpUrl newUrl = rewrite(url, proxy);
        if (newUrl == null) {
            Timber.w("AppApiProxyInterceptor: 代理地址非法已忽略: %s", proxy);
            return chain.proceed(request);
        }

        Timber.d("AppApiProxy → %s %s", request.method(), newUrl);
        return chain.proceed(request.newBuilder().url(newUrl).build());
    }

    public static HttpUrl rewrite(HttpUrl original, String proxy) {
        if (proxy == null || proxy.trim().isEmpty()) return null;

        final String prefix;
        if (APP_API_HOST.equals(original.host())) {
            prefix = "/pixiv-app-api";
        } else if (OAUTH_HOST.equals(original.host())) {
            prefix = "/pixiv-oauth";
        } else {
            return null;
        }

        final String baseStr = normalizeBase(proxy);
        if (baseStr == null) return null;
        final HttpUrl base;
        try {
            base = HttpUrl.get(baseStr);
        } catch (IllegalArgumentException e) {
            return null;
        }

        final String root = base.scheme() + "://" + base.host()
                + (base.port() != HttpUrl.defaultPort(base.scheme()) ? ":" + base.port() : "");
        String basePath = base.encodedPath();
        while (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        if (basePath.endsWith(prefix)) {
            basePath = basePath.substring(0, basePath.length() - prefix.length());
        }

        final String newUrlStr = root + basePath + prefix + original.encodedPath()
                + (original.encodedQuery() != null && !original.encodedQuery().isEmpty()
                        ? "?" + original.encodedQuery() : "");
        try {
            return HttpUrl.get(newUrlStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String normalizeBase(String proxy) {
        String p = proxy == null ? "" : proxy.trim();
        if (p.isEmpty()) return null;

        if (p.contains("://")) {
            final boolean https = p.regionMatches(true, 0, "https://", 0, 8);
            final boolean debugHttp = BuildConfig.DEBUG && p.regionMatches(true, 0, "http://", 0, 7);
            if (!https && !debugHttp) {
                return null;
            }
        } else {
            p = "https://" + p;
        }

        final HttpUrl url;
        try {
            url = HttpUrl.get(p);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (url.query() != null || url.fragment() != null) {
            return null;
        }

        String s = url.toString();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
