package rumpledcode.mashapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

/**
 * Created by Andro on 4.2.2018..
 */

public class LuckySongAdded extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lucky_song_added);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int)(width * 0.75), (int)(height * 0.3));
    }

    @Override
    protected void onStop() {
        Intent intent=new Intent();
        setResult(1337,intent);
        super.onStop();
    }
}