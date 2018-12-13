package wpi.team1021.scavengerhunt;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Date;
import java.util.Random;

import wpi.team1021.scavengerhunt.huntDB.utils.DatabaseUtils;

public class MainActivity extends AppCompatActivity {

    private String TAG = "wpi.team1021.scavengerhunt";
    final Context context = this;
    private MainActivityViewModel mMainActivityViewModel;
    private TextView mHuntHistoryList;
    private String username;
    private String randId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHuntHistoryList = findViewById(R.id.hunt_history_list);

        mMainActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        populateDb();

        subscribeUiHuntHistory();

    }

    public void onClickStartButton(View view) {

        // load the dialog_prompt_user.xml layout and inflate to view
        LayoutInflater layoutinflater = LayoutInflater.from(context);
        View promptUserView = layoutinflater.inflate(R.layout.dialog_prompt_user, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setView(promptUserView);

        final EditText userAnswer = (EditText) promptUserView.findViewById(R.id.usernameInput);

        alertDialogBuilder.setTitle("What's your username?");

        // prompt for username
        alertDialogBuilder.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // and display the username on main activity layout
                username = userAnswer.getText().toString();
                new AddHuntToDbASync().execute(username);
                Intent i = new Intent(MainActivity.this, CaptureAndInference.class);
                i.putExtra("RANDOM_ID", randId);
                startActivity(i);
            }
        });

        // all set and time to build and show up!
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();


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

    public void onClickRefreshHistory(View view) {
        populateDb();
        subscribeUiHuntHistory();
    }

    private class AddHuntToDbASync extends AsyncTask<String, Void, String> {

        protected void onPreExecute() {
            // Generate random id string
            Random generator = new Random();
            StringBuilder randomStringBuilder = new StringBuilder();
            char tempChar;
            for (int i = 0; i < 5; i++){
                tempChar = (char) (generator.nextInt(26) + 97);
                randomStringBuilder.append(tempChar);
            }
            randId = randomStringBuilder.toString();
        }

        protected String doInBackground(String... username) {
            mMainActivityViewModel.addHunt(randId, username[0], new Date());
            return randId;
        }
    }
}
