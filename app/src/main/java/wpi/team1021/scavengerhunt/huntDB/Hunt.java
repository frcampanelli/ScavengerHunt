package wpi.team1021.scavengerhunt.huntDB;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

@Entity
@TypeConverters(DateConverter.class)
public class Hunt {
    @PrimaryKey
    @NonNull
    public String id;

    public Date startTime;

    public String name;

    @ColumnInfo(name="points")
    public int points;
}
