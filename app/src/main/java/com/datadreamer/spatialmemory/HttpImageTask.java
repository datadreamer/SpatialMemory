package com.datadreamer.spatialmemory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Threaded task to download an image.
 */
public class HttpImageTask extends AsyncTask<String, Void, Bitmap> {
    private static final String TAG = "GetImageTask";
    private HttpTaskListener listener;
    private String id;

    public HttpImageTask(HttpTaskListener listener, String id){
        this.listener = listener;
        this.id = id;
    }

    @Override
    protected Bitmap doInBackground(String... urls) {
        try {
            return downloadUrl(urls[0]);
        } catch (IOException e) {
            return null;
        }
    }

    // callback to listeners with string result
    protected void onPostExecute(Bitmap result) {
        Log.d(TAG, "Image downloaded.");
        listener.httpImageDownloaded(result, id);
    }

    // GET url and return resulting content as string.
    private Bitmap downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();
            Bitmap img = readImage(is);
            return img;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    //Reads an InputStream and converts it to a Bitmap.
    public Bitmap readImage(InputStream stream) throws IOException, UnsupportedEncodingException {
        return BitmapFactory.decodeStream(stream);
    }
}
