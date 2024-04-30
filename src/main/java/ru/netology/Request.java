package ru.netology;

public class Request {
    private String method;
    private String title;
    private String body;

    public Request(String method, String title) {
        this.method = method;
        this.title = title;
    }

    public Request(String method, String title, String body) {
        this.method = method;
        this.title = title;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }
}
