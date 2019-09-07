package com.virjar.geetest.crack.net;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 探测字符集 Created by virjar on 16/9/20.
 */
public class CharsetDetector {
    /**
     * 通过头部解析
     * 
     * @param headers
     * @return
     */
    public static String detectHeader(Header[] headers) {
        if (headers == null || headers.length == 0) {
            return null;
        }
        for (Header header : headers) {
            String s = parseContentType(header.getValue());
            if (!StringUtils.isEmpty(s)) {
                return s;
            }
        }
        return null;
    }

    public static String detectHeader(Header header) {
        if (header == null) {
            return null;
        }
        String s = parseContentType(header.getValue());
        if (!StringUtils.isEmpty(s)) {
            return s;
        }
        return null;
    }

    public static String[] substringsBetween(final byte[] str, final byte[] open, final byte[] close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        final int strLen = str.length;
        if (strLen == 0) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        final int closeLen = close.length;
        final int openLen = open.length;
        final List<String> list = new ArrayList<>();
        int pos = 0;
        while (pos < strLen - closeLen) {
            int start = indexOf(str, 0, str.length, open, 0, open.length, pos);
            if (start < 0) {
                break;
            }
            start += openLen;
            final int end = indexOf(str, 0, str.length, close, 0, close.length, start);
            if (end < 0) {
                break;
            }
            // 这里就不考虑编码了
            list.add(new String(Arrays.copyOfRange(str, start, end)));
            pos = end + closeLen;
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.toArray(new String[list.size()]);
    }

    private static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset,
            int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        byte first = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (source[i] != first) {
                while (++i <= max && source[i] != first)
                    ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++)
                    ;

                if (j == end) {
                    /* Found whole string. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }

    /**
     * 通过响应内容解析,底层支持,要求高速处理
     * 
     * @param content
     * @return
     */
    public static String detectHtmlContent(final byte[] content) {
        String[] metas = substringsBetween(content, "<meta".getBytes(), ">".getBytes());// #4
        if (metas == null) {
            return null;
        }
        for (String meta : metas) {
            if (meta.toLowerCase().contains("Content-Type".toLowerCase())) {
                String contentType = parseContentType(StringUtils.substringBetween(meta, "content=\"", "\""));
                if (contentType != null) {
                    return contentType;
                }
            } else if (meta.contains("charset=\"")) {// html5 <meta charset="UTF-8" />
                int i = meta.indexOf("charset=\"");//需要继续优化
                String substring = meta.substring(i + 9);
                int end = substring.indexOf("\"");
                if (end > 0) {
                    return substring.substring(0, end);
                }
            }
        }
        return null;
    }

    private static String parseContentType(String contentType) {
        if (StringUtils.isEmpty(contentType)) {
            return null;
        }
        contentType = contentType.toLowerCase();
        int charset = contentType.indexOf("charset");
        if (charset < 0) {
            return null;
        }
        contentType = contentType.substring(charset + 7).trim();
        if (contentType.startsWith("=")) {
            return contentType.substring(1);
        }
        return contentType;
    }
}
