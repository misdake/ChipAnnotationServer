package com.rs.tool.chipannotation.log;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class Http {
    private final static Proxy        proxy  = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 1080));
    private final static OkHttpClient client = new OkHttpClient.Builder()
            .proxy(proxy)
            .build();
    public final static  Gson         gson   = new Gson();

    public static String get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String string = response.body() != null ? response.body().string() : null;
            return string;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T get(String url, Class<T> clazz) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        int retry = 3;
        while (--retry >= 0) {
            try (Response response = client.newCall(request).execute()) {
                String string = response.body() != null ? response.body().string() : null;
                if (response.code() == 403) {
                    System.out.println("403!");
                    System.out.println(string);
                }
                String apiRemaining = response.header("X-RateLimit-Remaining");
                String apiLimit = response.header("X-RateLimit-Limit");
                if (apiRemaining != null) System.out.println("    api left: " + apiRemaining + "/" + apiLimit);
                T r = gson.fromJson(string, clazz);
                return r;
            } catch (Exception e) {
                System.err.println("retry left: " + retry + "url :" + url);
                //noinspection ThrowablePrintedToSystemOut
                System.out.println(e);
            }
        }
        return null;
    }

}
