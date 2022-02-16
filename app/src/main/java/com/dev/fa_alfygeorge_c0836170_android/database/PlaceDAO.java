package com.dev.fa_alfygeorge_c0836170_android.database;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.dev.fa_alfygeorge_c0836170_android.model.Place;
import java.util.List;

@Dao
public interface PlaceDAO {

    @Insert(onConflict = REPLACE)
    void insert(Place place);

    @Query("SELECT * FROM Place ORDER BY id DESC")
    List<Place> getAllPlaces();

    @Query("UPDATE Place SET placeName = :placeName, createdDate = :createdDate,latitude= :latitude,longitude= :longitude,isVisited = :isVisited WHERE id = :id")
    void update(int id,String placeName,String createdDate,double latitude,double longitude,boolean isVisited);

    @Delete
    void delete(Place place);

}
