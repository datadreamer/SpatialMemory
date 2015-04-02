package com.datadreamer.spatialmemory;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Threaded task to download data from the API.
 */
public class HttpTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "GetAPITask";
    private HttpTaskListener listener;

    public HttpTask(HttpTaskListener listener){
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            return downloadUrl(urls[0]);
        } catch (IOException e) {
            return "Unable to retrieve web page. URL may be invalid.";
        }
    }

    // callback to listeners with string result
    protected void onPostExecute(String result) {
        listener.httpDataDownloaded(result);
    }

    // GET url and return resulting content as string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        int len = 8192;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();
            String contentAsString = readString(is, len);
            return contentAsString;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    public String readString(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String data = "";
        String line;
        while((line = reader.readLine()) != null){
            data += line;
        }
        reader.close();
        return data;
    }
}
