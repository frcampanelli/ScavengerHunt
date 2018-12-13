package wpi.team1021.scavengerhunt;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private MainActivityViewModel mMainActivityViewModel;

    private TextView mHuntHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHuntHistoryList = (TextView) findViewById(R.id.hunt_history_list);

        mMainActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        populateDb();

        subscribeUiHuntHistory();
    }

    private void populateDb() {
        mMainActivityViewModel.createDb();
    }

    private void subscribeUiHuntHistory() {
        mMainActivityViewModel.getHuntHistoryResult().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String result) {
                mHuntHistoryList.setText(result);
            }
        });
    }

    public void onRefreshHistoryClicked(View view) {
        populateDb();
        subscribeUiHuntHistory();
    }
}
