package com.technikh.imagetextgrabber.room.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.technikh.imagetextgrabber.models.VisionWordModel;

import java.util.ArrayList;
import java.util.List;

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

    @ColumnInfo
    public String text;

    public Images(){}

    public Images(int left, int top, int right, int bottom, String mtext, String color,String forImage) {
        this.bottom=bottom;
        this.top=top;
        this.left=left;
        this.right=right;
        this.text=mtext;
        this.color=color;
        this.forImage=forImage;
    }


}
