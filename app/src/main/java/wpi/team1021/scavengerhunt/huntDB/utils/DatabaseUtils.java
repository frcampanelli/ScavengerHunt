package wpi.team1021.scavengerhunt.huntDB.utils;

import android.os.AsyncTask;
import android.util.Log;

import wpi.team1021.scavengerhunt.huntDB.AppDatabase;
import wpi.team1021.scavengerhunt.huntDB.Hunt;

import java.util.Calendar;
import java.util.Date;

public class DatabaseUtils {

    // Simulate a blocking operation delaying each Hunt insertion with a delay:
    private static final int DELAY_MILLIS = 500;

    public static void populateAsync(final AppDatabase db) {

        PopulateDbAsync task = new PopulateDbAsync(db);
        task.execute();
    }

    private static Hunt addHunt(final AppDatabase db, final String id, final Date startTime,
                                final String name, final int points) {
        Hunt hunt = new Hunt();
        hunt.id = id;
        hunt.startTime = startTime;
        hunt.name = name;
        hunt.points = points;
        db.huntModel().insertHunts(hunt);
        return hunt;
    }

    private static void populateWithTestData(AppDatabase db) {
        db.huntModel().deleteAll();


        // Tickets are added with a delay, to have time for the UI to react to changes.
        try {
            Date today = getTodayPlusDays(0);
            Date yesterday = getTodayPlusDays(-1);
            Date lastWeek = getTodayPlusDays(-7);

            addHunt(db, "1", today, "Frank", 69);
            Thread.sleep(DELAY_MILLIS);
            addHunt(db, "2", yesterday, "Erick", 420);
            Thread.sleep(DELAY_MILLIS);
            addHunt(db, "3", lastWeek, "Carl", 3);
            Log.d("DB", "Added hunts");
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static Date getTodayPlusDays(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, daysAgo);
        return calendar.getTime();
    }

    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final AppDatabase mDb;

        PopulateDbAsync(AppDatabase db) {
            mDb = db;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            populateWithTestData(mDb);
            return null;
        }

    }
}
