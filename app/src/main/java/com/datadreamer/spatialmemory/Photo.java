package com.datadreamer.spatialmemory;

/**
 * Contains properties of archived photo entries.
 */
public class Photo {

    private int id, item_id, page_id;
    private String collection_id, title, circa;
    private float lat, lon;

    public Photo(int id, int item_id, int page_id, String collection_id, String title, String circa, float lat, float lon){
        this.id = id;
        this.item_id = item_id;
        this.page_id = page_id;
        this.collection_id = collection_id;
        this.title = title;
        this.circa = circa;
        this.lat = lat;
        this.lon = lon;
    }

}
