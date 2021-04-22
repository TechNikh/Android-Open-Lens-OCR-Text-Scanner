package com.technikh.imagetextgrabber.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Images {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    public String forImage;

    @ColumnInfo
    public int left;

    @ColumnInfo
    public int right;

    @ColumnInfo
    public int top;

    @ColumnInfo
    public int bottom;

    @ColumnInfo
    public String color;

    
    @androidx.annotation.NonNull
    @ColumnInfo
    public String note;

    @ColumnInfo
    public String text;

    public Images(){}

    public Images(int left, int top, int right, int bottom, String mtext, String color,String forImage,String note) {
        this.note=note;
        this.bottom=bottom;
        this.top=top;
        this.left=left;
        this.right=right;
        this.text=mtext;
        this.color=color;
        this.forImage=forImage;
    }


}
