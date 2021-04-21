package com.technikh.imagetextgrabber.room.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Highlights {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo
    public String color;
}
