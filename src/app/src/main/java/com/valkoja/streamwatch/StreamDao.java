package com.valkoja.streamwatch;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

@Dao
public interface StreamDao
{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertStream(Stream... streams);

    @Update
    public void updateStreams(Stream... streams);

    @Delete
    public void deleteStreams(Stream... streams);

    @Query("SELECT * FROM stream ORDER BY online DESC, name DESC")
    public Stream[] selectForListing();

    @Query("SELECT * FROM stream ORDER BY id DESC")
    public Stream[] selectForSettings();
}
