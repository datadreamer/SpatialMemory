package com.datadreamer.spatialmemory;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;


public class PhotoActivity extends ActionBarActivity implements HttpTaskListener{

    protected static final String TAG = "PhotoActivity";
    private HttpImageTask imageTask;
    private String apiUrl = "http://www.spatialmemory.com/api.py";
    private String id, title, circa;
    private Bitmap img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // not sure if this is a hack, but it works.
        if(isTaskRoot()){
            Intent intent = new Intent(this, PhotoActivity.class);
            intent.putExtras(getIntent());
            TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(intent)
                    .startActivities();
            return;
        }
        setContentView(R.layout.activity_photo);
        // grab photo data
        Bundle extras = getIntent().getExtras();
        String id = extras.getString("id");
        String title = extras.getString("title");
        String circa = extras.getString("circa");
        Log.d(TAG, id +": "+ title +" ("+ circa +")");
        // grab screen resolution
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int sh = metrics.heightPixels;
        int sw = metrics.widthPixels;
        // grab image
        String[] args = {id};
        imageTask = new HttpImageTask(this, args);
        imageTask.execute(apiUrl + "?action=photo&id=" + id +"&sw="+sw+"&sh="+sh);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void httpDataDownloaded(String result) {

    }

    @Override
    public void httpImageDownloaded(Bitmap img, String[] args) {
        ImageView iv = (ImageView)findViewById(R.id.imageView);
        iv.setImageBitmap(img);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Log.d(TAG, "Displaying photo: "+ args[0]);
    }
}
