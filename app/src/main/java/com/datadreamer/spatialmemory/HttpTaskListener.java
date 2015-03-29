package com.datadreamer.spatialmemory;

/**
 * HttpTaskListener must be implemented to receive results from an HttpAsyncTask.
 */
public interface HttpTaskListener{
    void httpTaskComplete(String result);
}
