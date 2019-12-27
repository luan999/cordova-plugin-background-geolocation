package com.marianhello.bgloc;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class HttpsPostService {
    public static final int BUFFER_SIZE = 1024;

    private String mUrl;
    private HttpsURLConnection mHttpsURLConnection;

    public HttpsPostService(String url) {
        mUrl = url;
    }

    private HttpsURLConnection openConnection() throws IOException {
        if (mHttpsURLConnection == null) {
            mHttpsURLConnection = (HttpsURLConnection) new URL(mUrl).openConnection();
        }
        return mHttpsURLConnection;
    }

    public JSONArray getJSON() throws IOException {
      HttpsURLConnection conn = this.openConnection();
      JSONArray json = null;
      try {
        // Create the SSL connection
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, null, new java.security.SecureRandom());
        conn.setSSLSocketFactory(sc.getSocketFactory());
        conn.setDoInput(true);
        conn.setReadTimeout(30 * 1000);
        conn.setConnectTimeout(30 * 1000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");

        int statusCode = conn.getResponseCode();

        if (statusCode == HttpURLConnection.HTTP_OK) {
          BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
          String line;
          StringBuffer s = new StringBuffer();
          while ((line = br.readLine()) != null) {
            s.append(line);
          }
          json = new JSONArray(s.toString());
          br.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return json;
    }

    public static JSONArray getJSON(String url) throws IOException {
        HttpsPostService service = new HttpsPostService(url);
        return service.getJSON();
    }
}
