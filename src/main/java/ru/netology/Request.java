package ru.netology;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class Request {
    private String method;
    private String path;
    private String body;
    private List<NameValuePair> queryParamList;

    public Request(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public Request(String method, String path, List<NameValuePair> queryParamList) {
        this.method = method;
        this.path = path;
        this.queryParamList = queryParamList;
    }

    public Request(String method, String title, String body) {
        this.method = method;
        this.path = title;
        this.body = body;
    }

    public Request(String method, String title, String body, List<NameValuePair> queryParamList) {
        this.method = method;
        this.path = title;
        this.body = body;
        this.queryParamList = queryParamList;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getBody() {
        return body;
    }

    public String getQueryParam(String title) {
        if (queryParamList != null) {
            for (int i = 0; i < queryParamList.size(); i++) {
                if (queryParamList.get(i).getName().equals(title)) {
                    return queryParamList.get(i).getName() + queryParamList.get(i).getValue();
                }
            }
        }
        return null;
    }

    public String getQueryParams() {
        if (queryParamList != null) {
            final var stringBuilder = new StringBuilder();
            for (int i = 0; i < queryParamList.size(); i++) {
                stringBuilder.append(queryParamList.get(i));
            }
            return stringBuilder.toString();
        }
        return null;
    }
}
