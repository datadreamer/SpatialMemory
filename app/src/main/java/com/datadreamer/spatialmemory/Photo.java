package com.datadreamer.spatialmemory;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contains properties of archived photo entries.
 */
public class Photo implements Parcelable {

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

    public Photo(Parcel source){
        this.id = source.readInt();
        this.item_id = source.readInt();
        this.page_id = source.readInt();
        this.collection_id = source.readString();
        this.title = source.readString();
        this.circa = source.readString();
        this.lat = source.readFloat();
        this.lon = source.readFloat();
    }

    public String getCirca(){
        return circa;
    }

    public int getID(){
        return id;
    }

    public String getTitle(){
        return title;
    }

    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(item_id);
        dest.writeInt(page_id);
        dest.writeString(collection_id);
        dest.writeString(title);
        dest.writeString(circa);
        dest.writeFloat(lat);
        dest.writeFloat(lon);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){
        public Photo createFromParcel(Parcel in){
            return new Photo(in);
        }
        public Photo[] newArray(int size){
            return new Photo[size];
        }
    };
}
