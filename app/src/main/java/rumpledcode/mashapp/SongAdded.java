package rumpledcode.mashapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

/**
 * Created by Andro on 4.2.2018..
 */

public class SongAdded extends AppCompatActivity {
    public int result = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_added);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int)(width * 0.75), (int)(height * 0.2));
    }

    @Override
    protected void onStop() {
        Intent intent=new Intent();
        setResult(result,intent);
        super.onStop();
    }
}