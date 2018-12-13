package wpi.team1021.scavengerhunt;

import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Transformations;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import wpi.team1021.scavengerhunt.huntDB.AppDatabase;
import wpi.team1021.scavengerhunt.huntDB.Hunt;
import wpi.team1021.scavengerhunt.huntDB.utils.DatabaseUtils;

public class MainActivityViewModel extends AndroidViewModel {

    private LiveData<String> mHuntHistoryResult;

    private AppDatabase mDb;

    public MainActivityViewModel(Application application) { super(application); }

    public void addHunt(String id, String username, Date startTime){
        DatabaseUtils.addHunt(mDb, id, startTime, username, 0);
    }
    public LiveData<String> getHuntHistoryResult() {
        return mHuntHistoryResult;
    }

    public void createDb() {
        mDb = AppDatabase.getInMemoryDatabase(getApplication());

        // Receive changes
        subscribeToDbChanges();
    }

    private void subscribeToDbChanges() {
        LiveData<List<Hunt>> hunts
                = mDb.huntModel().getAllHunts();

        // Instead of exposing the list of Tickets, we can apply a transformation and expose Strings.
        mHuntHistoryResult = Transformations.map(hunts,
                new Function<List<Hunt>, String>() {
                    @Override
                    public String apply(List<Hunt> huntList) {
                        StringBuilder sb = new StringBuilder();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss",
                                Locale.US);

                        for (Hunt hunt : huntList) {
                            sb.append(String.format(Locale.US,
                                    "%s\u0009|" +
                                            "%s\u0009|" +
                                            "%s\u0009|" +
                                            "%d points|\n\n",
                                    hunt.id,
                                    simpleDateFormat.format(hunt.startTime),
                                    hunt.name,
                                    hunt.points));
                        }
                        return sb.toString();
                    }
                });
    }
}
