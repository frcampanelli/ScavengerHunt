package wpi.team1021.scavengerhunt.huntDB;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.Update;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;
import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
@TypeConverters(DateConverter.class)
public interface HuntDao {

    @Query("select * from Hunt where id = :id")
    Hunt loadHuntsById(int id);

    @Query("SELECT * FROM Hunt")
    LiveData<List<Hunt>> getAllHunts();
    
    @Query("SELECT * FROM Hunt ORDER BY points DESC")
    LiveData<List<Hunt>> getAllHuntsByMostPoints();

    @Query("UPDATE Hunt SET points=:points WHERE id = :id")
    void updatePointsById(int id, int points);

    @Query("SELECT * FROM Hunt")
    List<Hunt> getAllHuntsSync();

    @Query("SELECT * FROM Hunt WHERE name = :name")
    List<Hunt> findAllHuntsByName(String name);
    
    @Insert(onConflict = IGNORE)
    void insertHunts(Hunt hunt);

    @Update(onConflict = REPLACE)
    void updateHunts(Hunt hunt);

    @Query("DELETE FROM Hunt")
    void deleteAll();
}
