package com.datadreamer.spatialmemory;

import android.graphics.Bitmap;

/**
 * HttpTaskListener must be implemented to receive results from an HttpTask.
 */
public interface HttpTaskListener{
    void httpDataDownloaded(String result);
    void httpImageDownloaded(Bitmap img, String id);
}
