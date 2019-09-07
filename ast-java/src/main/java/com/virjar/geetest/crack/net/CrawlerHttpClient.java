package com.virjar.geetest.crack.net;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthState;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParamConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.Lookup;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpParamsNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 包装httpclient,应该继承它 Created by virjar on 16/9/19.
 */
public class CrawlerHttpClient extends CloseableHttpClient implements Configurable {

    private final Log log = LogFactory.getLog(getClass());

    private final ClientExecChain execChain;
    private final HttpClientConnectionManager connManager;
    private final HttpRoutePlanner routePlanner;
    private final Lookup<CookieSpecProvider> cookieSpecRegistry;
    private final Lookup<AuthSchemeProvider> authSchemeRegistry;
    private final CookieStore cookieStore;
    private final CredentialsProvider credentialsProvider;
    private final RequestConfig defaultConfig;
    private final List<Closeable> closeables;

    CrawlerHttpClient(final ClientExecChain execChain, final HttpClientConnectionManager connManager,
            final HttpRoutePlanner routePlanner, final Lookup<CookieSpecProvider> cookieSpecRegistry,
            final Lookup<AuthSchemeProvider> authSchemeRegistry, final CookieStore cookieStore,
            final CredentialsProvider credentialsProvider, final RequestConfig defaultConfig,
            final List<Closeable> closeables) {
        super();
        Args.notNull(execChain, "HTTP client exec chain");
        Args.notNull(connManager, "HTTP connection manager");
        Args.notNull(routePlanner, "HTTP route planner");
        this.execChain = execChain;
        this.connManager = connManager;
        this.routePlanner = routePlanner;
        this.cookieSpecRegistry = cookieSpecRegistry;
        this.authSchemeRegistry = authSchemeRegistry;
        this.cookieStore = cookieStore;
        this.credentialsProvider = credentialsProvider;
        this.defaultConfig = defaultConfig;
        this.closeables = closeables;
    }

    private HttpRoute determineRoute(final HttpHost target, final HttpRequest request, final HttpContext context)
            throws HttpException {
        HttpHost host = target;
        if (host == null) {
            host = (HttpHost) request.getParams().getParameter(ClientPNames.DEFAULT_HOST);
        }
        return this.routePlanner.determineRoute(host, request, context);
    }

    private void setupContext(final HttpClientContext context) {
        if (context.getAttribute(HttpClientContext.TARGET_AUTH_STATE) == null) {
            context.setAttribute(HttpClientContext.TARGET_AUTH_STATE, new AuthState());
        }
        if (context.getAttribute(HttpClientContext.PROXY_AUTH_STATE) == null) {
            context.setAttribute(HttpClientContext.PROXY_AUTH_STATE, new AuthState());
        }
        if (context.getAttribute(HttpClientContext.AUTHSCHEME_REGISTRY) == null) {
            context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, this.authSchemeRegistry);
        }
        if (context.getAttribute(HttpClientContext.COOKIESPEC_REGISTRY) == null) {
            context.setAttribute(HttpClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);
        }
        if (context.getAttribute(HttpClientContext.COOKIE_STORE) == null) {
            context.setAttribute(HttpClientContext.COOKIE_STORE, this.cookieStore);
        }
        if (context.getAttribute(HttpClientContext.CREDS_PROVIDER) == null) {
            context.setAttribute(HttpClientContext.CREDS_PROVIDER, this.credentialsProvider);
        }
        if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
            context.setAttribute(HttpClientContext.REQUEST_CONFIG, this.defaultConfig);
        }
    }

    @Override
    protected CloseableHttpResponse doExecute(final HttpHost target, final HttpRequest request,
            final HttpContext context) throws IOException {
        Args.notNull(request, "HTTP request");
        HttpExecutionAware execAware = null;
        if (request instanceof HttpExecutionAware) {
            execAware = (HttpExecutionAware) request;
        }
        try {
            final HttpRequestWrapper wrapper = HttpRequestWrapper.wrap(request, target);
            final HttpClientContext localcontext = HttpClientContext
                    .adapt(context != null ? context : new BasicHttpContext());
            RequestConfig config = null;
            if (request instanceof Configurable) {
                config = ((Configurable) request).getConfig();
            }
            if (config == null) {
                final HttpParams params = request.getParams();
                if (params instanceof HttpParamsNames) {
                    if (!((HttpParamsNames) params).getNames().isEmpty()) {
                        config = HttpClientParamConfig.getRequestConfig(params);
                    }
                } else {
                    config = HttpClientParamConfig.getRequestConfig(params);
                }
            }
            if (config != null) {
                localcontext.setRequestConfig(config);
            }
            setupContext(localcontext);
            final HttpRoute route = determineRoute(target, wrapper, localcontext);
            return this.execChain.execute(route, wrapper, localcontext, execAware);
        } catch (final HttpException httpException) {
            throw new ClientProtocolException(httpException);
        }
    }

    @Override
    public RequestConfig getConfig() {
        return this.defaultConfig;
    }

    @Override
    public void close() {
        if (this.closeables != null) {
            for (final Closeable closeable : this.closeables) {
                try {
                    closeable.close();
                } catch (final IOException ex) {
                    this.log.error(ex.getMessage(), ex);
                }
            }
        }
    }

    @Override
    public HttpParams getParams() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {

        return new ClientConnectionManager() {

            @Override
            public void shutdown() {
                connManager.shutdown();
            }

            @Override
            public ClientConnectionRequest requestConnection(final HttpRoute route, final Object state) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void releaseConnection(final ManagedClientConnection conn, final long validDuration,
                    final TimeUnit timeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SchemeRegistry getSchemeRegistry() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void closeIdleConnections(final long idletime, final TimeUnit tunit) {
                connManager.closeIdleConnections(idletime, tunit);
            }

            @Override
            public void closeExpiredConnections() {
                connManager.closeExpiredConnections();
            }

        };

    }

    public String get(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return get(url, convert(params), charset, headers, proxyIp, proxyPort);
    }

    public String get(String url, Charset charset, Header[] headers, String proxyIp, int proxyPort) {
        return get(url, (List<NameValuePair>) null, charset, headers, proxyIp, proxyPort);
    }

    public String get(String url, List<NameValuePair> nameValuePairs, Header[] headers, String proxyIp, int proxyPort) {
        return get(url, nameValuePairs, null, headers, proxyIp, proxyPort);
    }

    public String get(String url, List<NameValuePair> params, Charset charset, String proxyIp, int proxyPort) {
        return get(url, params, charset, null, proxyIp, proxyPort);
    }

    public String get(String url, List<NameValuePair> params, Charset charset, Header[] headers) {
        return get(url, params, charset, headers, null, -1);
    }

    public String get(String url, Header[] headers, String proxyIp, int proxyPort) {
        return get(url, (List<NameValuePair>) null, null, headers, proxyIp, proxyPort);
    }

    public String get(String url, Charset charset, String proxyIp, int proxyPort) {
        return get(url, (List<NameValuePair>) null, charset, null, proxyIp, proxyPort);
    }

    public String get(String url, Charset charset, Header[] headers) {
        return get(url, (List<NameValuePair>) null, charset, headers, null, -1);
    }

    public String get(String url, List<NameValuePair> params, String proxyIp, int proxyPort) {
        return get(url, params, null, null, proxyIp, proxyPort);
    }

    public String get(String url, List<NameValuePair> params, Header[] headers) {
        return get(url, params, null, headers, null, -1);
    }

    public String get(String url, List<NameValuePair> params, Charset charset) {
        return get(url, params, charset, null, null, -1);
    }

    public String get(String url, List<NameValuePair> params) {
        return get(url, params, null, null, null, -1);
    }

    public String get(String url, Charset charset) {
        return get(url, (List<NameValuePair>) null, charset, null, null, -1);
    }

    public String get(String url, Header[] headers) {
        return get(url, (List<NameValuePair>) null, null, headers, null, -1);
    }

    public String get(String url, String proxyIp, int proxyPort) {
        return get(url, (List<NameValuePair>) null, null, null, proxyIp, proxyPort);
    }

    public String get(String url) {
        return get(url, (List<NameValuePair>) null, null, null, null, -1);
    }

    public String get(String url, HttpClientContext httpClientContext) {
        return get(url, null, null, null, null, -1, httpClientContext);
    }

    public int getStatus(String url, String proxyIp, int proxyPort) {
        HttpGet httpGet = new HttpGet(url);
        RequestConfig.Builder builder = RequestConfig.custom().setSocketTimeout(ProxyConstant.SOCKET_TIMEOUT)
                .setConnectTimeout(ProxyConstant.CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(ProxyConstant.REQUEST_TIMEOUT).setRedirectsEnabled(true)
                .setCircularRedirectsAllowed(true);
        if (StringUtils.isNotEmpty(proxyIp)) {
            builder.setProxy(new HttpHost(proxyIp, proxyPort));
        }
        httpGet.setConfig(builder.build());
        try {
            CloseableHttpResponse execute = execute(httpGet);
            return execute.getStatusLine().getStatusCode();
        } catch (IOException e) {
            return -1;
        } finally {
            httpGet.abort();
            // httpGet.releaseConnection(); //看起来有链接没有关闭的问题发生,尝试替换这个试一试
        }
    }

    public String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return get(url, params, charset, headers, proxyIp, proxyPort, null);
    }

    public String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort, HttpClientContext httpClientContext) {
        if (params != null && params.size() > 0) {
            url = url + "?" + URLEncodedUtils.format(params, "utf-8");
        }
        HttpGet httpGet = new HttpGet(url);
        RequestConfig.Builder builder = RequestConfig.custom().setSocketTimeout(ProxyConstant.SOCKET_TIMEOUT)
                .setConnectTimeout(ProxyConstant.CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(ProxyConstant.REQUEST_TIMEOUT).setRedirectsEnabled(true)
                .setCircularRedirectsAllowed(true);
        if (StringUtils.isNotEmpty(proxyIp)) {
            builder.setProxy(new HttpHost(proxyIp, proxyPort));
        }
        httpGet.setConfig(builder.build());

        if (headers != null && headers.length > 0) {
            httpGet.setHeaders(headers);
        }
        try {
            return decodeHttpResponse(execute(httpGet, httpClientContext), charset);
        } catch (IOException e) {
            return null;
        }
    }

    public byte[] getEntity(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
                      int proxyPort, HttpClientContext httpClientContext) {
        if (params != null && params.size() > 0) {
            url = url + "?" + URLEncodedUtils.format(params, "utf-8");
        }
        HttpGet httpGet = new HttpGet(url);
        RequestConfig.Builder builder = RequestConfig.custom().setSocketTimeout(ProxyConstant.SOCKET_TIMEOUT)
                .setConnectTimeout(ProxyConstant.CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(ProxyConstant.REQUEST_TIMEOUT).setRedirectsEnabled(true)
                .setCircularRedirectsAllowed(true);
        if (StringUtils.isNotEmpty(proxyIp)) {
            builder.setProxy(new HttpHost(proxyIp, proxyPort));
        }
        httpGet.setConfig(builder.build());

        if (headers != null && headers.length > 0) {
            httpGet.setHeaders(headers);
        }
        try {
            return EntityUtils.toByteArray(execute(httpGet, httpClientContext).getEntity());
        } catch (IOException e) {
            return null;
        }
    }

    public byte[] getEntity(String url) {
        return  getEntity(url,null,null,null,null,0,null);
    }

    public String post(String url, String entity, Charset charset, Header[] headers, String proxyIp, int proxyPort) {
        return post(url, new StringEntity(entity, ContentType.TEXT_PLAIN), charset, headers, proxyIp, proxyPort);
    }

    public String postJSON(String url, Object entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return post(url, new StringEntity(JSONObject.toJSONString(entity), ContentType.APPLICATION_JSON), charset,
                headers, proxyIp, proxyPort);
    }

    public String post(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return post(url, new UrlEncodedFormEntity(params, Charset.defaultCharset()), charset, headers, proxyIp,
                proxyPort);
    }

    public String post(String url, List<NameValuePair> params, Header[] headers) {
        return post(url, new UrlEncodedFormEntity(params, Charset.defaultCharset()), null, headers, null, -1);
    }

    public String post(String url, Map<String, String> params, Header[] headers) {
        return post(url, new UrlEncodedFormEntity(convert(params), Charset.defaultCharset()), null, headers, null, -1);
    }

    public String post(String url, String entity, Header[] headers) {
        return post(url, new StringEntity(entity, ContentType.create("text/plain", Charset.defaultCharset())), null,
                headers, null, -1);
    }

    public String postJSON(String url, Object entity, Header[] headers) {
        return post(url, new StringEntity(JSONObject.toJSONString(entity), ContentType.APPLICATION_JSON), null, headers,
                null, -1);
    }

    public String post(String url, Map<String, String> params) {
        return post(url, new UrlEncodedFormEntity(convert(params), Charset.defaultCharset()), null, null, null, -1);
    }

    public String post(String url, String entity) {
        return post(url, new StringEntity(entity, ContentType.create("text/plain", Charset.defaultCharset())), null,
                null, null, -1);
    }

    public String postJSON(String url, Object entity) {
        return post(url, new StringEntity(JSONObject.toJSONString(entity), ContentType.APPLICATION_JSON), null, null,
                null, -1);
    }

    public String post(String url, List<NameValuePair> params) {
        return post(url, new UrlEncodedFormEntity(params, Charset.defaultCharset()), null, null, null, -1);
    }

    public String post(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return post(url, new UrlEncodedFormEntity(convert(params), Charset.defaultCharset()), charset, headers, proxyIp,
                proxyPort);
    }

    public String post(String url, HttpEntity entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {

        HttpPost httpPost = new HttpPost(url);
        RequestConfig.Builder builder = RequestConfig.custom().setSocketTimeout(ProxyConstant.SOCKET_TIMEOUT)
                .setConnectTimeout(ProxyConstant.CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(ProxyConstant.REQUEST_TIMEOUT).setRedirectsEnabled(true)
                .setCircularRedirectsAllowed(true);

        if (StringUtils.isNotEmpty(proxyIp)) {
            builder.setProxy(new HttpHost(proxyIp, proxyPort));
        }
        httpPost.setConfig(builder.build());
        if (headers != null && headers.length > 0) {
            httpPost.setHeaders(headers);
        }
        httpPost.setEntity(entity);
        try {
            return decodeHttpResponse(execute(httpPost), charset);
        } catch (IOException e) {
            return null;
        }

    }

    private String decodeHttpResponse(CloseableHttpResponse response, Charset charset) throws IOException {
        byte[] bytes = EntityUtils.toByteArray(response.getEntity());
        if (charset == null) {
            Header contentType = response.getFirstHeader("Content-Type");
            if (contentType != null) {
                String charsetStr = CharsetDetector.detectHeader(contentType);
                if (charsetStr != null) {
                    charset = Charset.forName(charsetStr);
                }
            }
        }
        if (charset == null) {
            String charsetStr = CharsetDetector.detectHtmlContent(bytes);
            if (charsetStr != null) {
                charset = Charset.forName(charsetStr);
            }
        }
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        return new String(bytes, charset);
    }

    private List<NameValuePair> convert(Map<String, String> params) {
        List<NameValuePair> nameValuePairs = Lists.newArrayList();
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        return nameValuePairs;
    }
}
