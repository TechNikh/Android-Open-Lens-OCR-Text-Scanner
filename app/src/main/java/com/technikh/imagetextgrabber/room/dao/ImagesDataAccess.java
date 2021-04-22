package com.technikh.imagetextgrabber.room.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.technikh.imagetextgrabber.room.entity.Images;

@Dao
public interface ImagesDataAccess {

    @Update
    public void update(Images image);

    @Insert
    public void insert(Images image);

    @Delete
    public void delete(Images image);

    @Query("SELECT * FROM IMAGES WHERE :name==forImage")
    public java.util.List<Images> getAllImage(String name);


}
