package com.technikh.imagetextgrabber.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.technikh.imagetextgrabber.room.entity.Highlights;
import com.technikh.imagetextgrabber.room.entity.Images;

@Database(entities = {Images.class, Highlights.class},version = 1)
public abstract class MyDatabase extends RoomDatabase {
    public abstract com.technikh.imagetextgrabber.room.dao.ImagesDataAccess getImagesDao();

    public abstract com.technikh.imagetextgrabber.room.dao.HighlightDataAccess getHighlightsDao();

}
