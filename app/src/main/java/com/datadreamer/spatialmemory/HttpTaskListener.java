package com.datadreamer.spatialmemory;

/**
 * HttpTaskListener must be implemented to receive results from an HttpTask.
 */
public interface HttpTaskListener{
    void httpTaskComplete(String result);
}
