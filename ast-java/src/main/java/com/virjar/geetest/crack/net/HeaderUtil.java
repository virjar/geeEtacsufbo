package com.virjar.geetest.crack.net;

import com.google.common.collect.Lists;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.List;

/**
 * 默认添加的请求头,具体是否需要根据方法添加,后续讨论<br/>
 * 源码追踪发现可能会覆盖requestConfig里面存在的header,待确认<br/>
 * Created by virjar on 16/9/28.
 */
public class HeaderUtil {
    public static final List<Header> defaultHeaders = Lists.newArrayList();
    static {
        defaultHeaders.add(new BasicHeader("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
        //这个默认会添加,值为 gzip, deflate
        //defaultHeaders.add(new BasicHeader("Accept-Encoding", "gzip, deflate, sdch, br"));
        defaultHeaders.add(new BasicHeader("Accept-Language", "en-US,en;q=0.8"));
        defaultHeaders.add(new BasicHeader("Cache-Control", "max-age=0"));
    }
}
