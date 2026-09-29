package com.jovan.supportcenter;

import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class ApiClient {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
    private final String baseUrl;
    private String token;

    public ApiClient(String baseUrl) { this.baseUrl = baseUrl.replaceAll("/$", ""); }
    public void setToken(String token) { this.token = token; }
    public String getToken() { return token; }

    private void call(String method, String path, String json, Callback callback, boolean authenticated) {
        Request.Builder b = new Request.Builder().url(baseUrl + path);
        if ("GET".equals(method)) b.get();
        else b.method(method, RequestBody.create(json == null ? "{}" : json, MediaType.parse("application/json")));
        if (authenticated && token != null) b.header("Authorization", "Bearer " + token);
        client.newCall(b.build()).enqueue(callback);
    }
    public void register(String json, Callback cb) { call("POST", "/auth/register", json, cb, false); }
    public void login(String json, Callback cb) { call("POST", "/auth/login", json, cb, false); }
    public void me(Callback cb) { call("GET", "/me", null, cb, true); }
    public void getPublicRequests(Callback cb) { call("GET", "/requests/public", null, cb, false); }
    public void createRequest(String json, Callback cb) { call("POST", "/requests", json, cb, true); }
    public void getMine(Callback cb) { call("GET", "/requests/mine", null, cb, true); }
    public void createContribution(String json, Callback cb) { call("POST", "/contributions", json, cb, true); }
    public void getContributions(Callback cb) { call("GET", "/contributions/mine", null, cb, true); }
    public void getNotifications(Callback cb) { call("GET", "/notifications", null, cb, true); }
}
