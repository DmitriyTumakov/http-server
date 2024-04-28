package ru.netology;

import org.apache.hc.core5.net.URLEncodedUtils;

import java.nio.charset.StandardCharsets;

public class Request {
    private String method;
    private String path;
    private String body;
    private String mimeType;

    public Request(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public Request(String method, String title, String body) {
        this.method = method;
        this.path = title;
        this.body = body;
    }

    public void addMimeType(String mimeType) {
        this.mimeType = mimeType;
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
        final var pathArray = path.split("\\?");
        if (pathArray.length == 2) {
            final var queryParamList = URLEncodedUtils.parse(pathArray[1], StandardCharsets.UTF_8);
            for (int i = 0; i < queryParamList.size(); i++) {
                if (queryParamList.get(i).getName().equals(title)) {
                    return queryParamList.get(i).getName() + ": " + queryParamList.get(i).getValue();
                }
            }
        }
        return null;
    }

    public String getQueryParams() {
        final var pathArray = path.split("\\?");
        if (pathArray.length == 2) {
            final var queryParamList = URLEncodedUtils.parse(pathArray[1], StandardCharsets.UTF_8);
            final var stringBuilder = new StringBuilder();
            for (int i = 0; i < queryParamList.size(); i++) {
                stringBuilder.append(queryParamList.get(i).getName() + ": " + queryParamList.get(i).getValue());
            }
            return stringBuilder.toString();
        }
        return null;
    }

    public String getPostParam(String name) {
        if (mimeType.equals("application/x-www-form-urlencoded")) {
            final var bodyParamList = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
            for (int i = 0; i < bodyParamList.size(); i++) {
                if (bodyParamList.get(i).getName().equals(name)) {
                    return bodyParamList.get(i).getName() + ": " + bodyParamList.get(i).getValue();
                }
            }
        }
        return null;
    }

    public String getPostParams() {
        if (mimeType.equals("application/x-www-form-urlencoded")) {
            final var bodyParamList = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
            final var stringBuilder = new StringBuilder();
            for (int i = 0; i < bodyParamList.size(); i++) {
                stringBuilder.append(bodyParamList.get(i).getName() + ": " + bodyParamList.get(i).getValue());
            }
            return stringBuilder.toString();
        }
        return null;
    }
}
