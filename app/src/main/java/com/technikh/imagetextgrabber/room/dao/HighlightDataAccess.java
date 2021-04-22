package com.technikh.imagetextgrabber.room.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.technikh.imagetextgrabber.room.entity.Highlights;

@Dao
public interface HighlightDataAccess {

    @Insert
    public void add(java.util.List<Highlights> colors);

    @Insert
    public void add(Highlights color);

    @Delete
    public void remove(java.util.List<Highlights> colors);

    @Delete
    public void remove(Highlights colors);

    @Query("SELECT * FROM highlights")
    public java.util.List<Highlights> getMarkers();


}
