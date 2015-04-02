package com.datadreamer.spatialmemory;

import android.graphics.Bitmap;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class PhotoActivity extends ActionBarActivity implements HttpTaskListener{

    private HttpImageTask httpImageTask;
    private String apiUrl = "http://www.spatialmemory.com/api.py";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        httpImageTask = new HttpImageTask(this);
        //httpImageTask.execute(apiUrl + "?action=photo&id=" + id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void httpDataDownloaded(String result) {

    }

    @Override
    public void httpImageDownloaded(Bitmap img) {

    }
}
