package rumpledcode.mashapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;

public class OutOfVotes extends AppCompatActivity {

    Button watchVideoButton;
    int resultCode = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.out_of_votes_menu);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int) (width * 0.75), (int) (height * 0.5));

        watchVideoButton = findViewById(R.id.watchVideoButton);

        watchVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultCode = 1;
                onStop();
            }
        });
    }

    @Override
    protected void onStop() {
        Intent intent = new Intent();
        setResult(resultCode, intent);
        super.onStop();
        finish();
    }

}