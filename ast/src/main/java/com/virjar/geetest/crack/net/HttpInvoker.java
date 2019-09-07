package com.virjar.geetest.crack.net;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.LaxRedirectStrategy;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;


/**
 * 以静态方式封装httpclient,方便的http请求客户端<br/>
 * Created by virjar on 16/10/4.
 */
public class HttpInvoker {
    private static CrawlerHttpClient crawlerHttpClient;


    static {// TODO 是否考虑cookie reject
        SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setSoLinger(-1).setSoReuseAddress(false)
                .setSoTimeout(ProxyConstant.SOCKETSO_TIMEOUT).setTcpNoDelay(true).build();
        X509TrustManager x509mgr = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] xcs, String string) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] xcs, String string) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{x509mgr}, null);
        } catch (Exception e) {
            //// TODO: 16/11/23  
        } 

        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        crawlerHttpClient = CrawlerHttpClientBuilder.create().setMaxConnTotal(1000).setMaxConnPerRoute(50)
                .setDefaultSocketConfig(socketConfig).setSSLSocketFactory(sslConnectionSocketFactory).setRedirectStrategy(new LaxRedirectStrategy()).build();
    }

    public static String postJSON(String url, Object entity, Header[] headers) {
        return crawlerHttpClient.postJSON(url, entity, headers);
    }

    public static String get(String url) {
        return crawlerHttpClient.get(url);
    }

    public static String get(String url, Charset charset) {
        return crawlerHttpClient.get(url, charset);
    }

    public static String get(String url, Charset charset, Header[] headers) {
        return crawlerHttpClient.get(url, charset, headers);
    }

    public static String get(String url, Charset charset, Header[] headers, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, Charset charset, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, charset, proxyIp, proxyPort);
    }

    public static String get(String url, Header[] headers) {
        return crawlerHttpClient.get(url, headers);
    }

    public static String get(String url, Header[] headers, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, headers, proxyIp, proxyPort);
    }

    public static String get(String url, HttpClientContext httpClientContext) {
        return crawlerHttpClient.get(url, httpClientContext);
    }

    public static String get(String url, List<NameValuePair> nameValuePairs, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.get(url, nameValuePairs, headers, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params) {
        return crawlerHttpClient.get(url, params);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset) {
        return crawlerHttpClient.get(url, params, charset);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers) {
        return crawlerHttpClient.get(url, params, charset, headers);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.get(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort, HttpClientContext httpClientContext) {
        return crawlerHttpClient.get(url, params, charset, headers, proxyIp, proxyPort, httpClientContext);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, params, charset, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params, Header[] headers) {
        return crawlerHttpClient.get(url, params, headers);
    }

    public static String get(String url, List<NameValuePair> params, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, params, proxyIp, proxyPort);
    }

    public static String get(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.get(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, proxyIp, proxyPort);
    }

    public static int getStatus(String url, String proxyIp, int proxyPort) {
        return crawlerHttpClient.getStatus(url, proxyIp, proxyPort);
    }

    public static String post(String url, HttpEntity entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.post(url, entity, charset, headers, proxyIp, proxyPort);
    }

    public static String post(String url, String entity) {
        return crawlerHttpClient.post(url, entity);
    }

    public static String post(String url, String entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.post(url, entity, charset, headers, proxyIp, proxyPort);
    }

    public static String post(String url, String entity, Header[] headers) {
        return crawlerHttpClient.post(url, entity, headers);
    }

    public static String post(String url, List<NameValuePair> params) {
        return crawlerHttpClient.post(url, params);
    }

    public static String post(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.post(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String post(String url, List<NameValuePair> params, Header[] headers) {
        return crawlerHttpClient.post(url, params, headers);
    }

    public static String post(String url, Map<String, String> params) {
        return crawlerHttpClient.post(url, params);
    }

    public static String post(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.post(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String post(String url, Map<String, String> params, Header[] headers) {
        return crawlerHttpClient.post(url, params, headers);
    }

    public static String postJSON(String url, Object entity) {
        return crawlerHttpClient.postJSON(url, entity);
    }

    public static String postJSON(String url, Object entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.postJSON(url, entity, charset, headers, proxyIp, proxyPort);
    }
}
