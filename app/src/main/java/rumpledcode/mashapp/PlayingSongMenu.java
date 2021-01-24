package rumpledcode.mashapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PlayingSongMenu extends AppCompatActivity {

    public int result = 1;
    TextView playingSong;
    TextView clipboardLabel;
    Button shareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing_song_menu);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int)(width * 0.75), (int)(height * 0.4));

        playingSong =       findViewById(R.id.playingSong);
        shareButton =       findViewById(R.id.shareButton);
        clipboardLabel =    findViewById(R.id.clipboardLabel);

        Bundle extras = getIntent().getExtras();
        playingSong.setText(extras.getString("SONG"));
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String shareMessage = "Listening to " + playingSong.getText() + " thanks to MashApp!";
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(intent, "Share"));
            }
        });
        playingSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("song", playingSong.getText());
                clipboard.setPrimaryClip(clip);
                clipboardLabel.setText("Copied!");
            }
        });
    }

    @Override
    protected void onStop() {
        Intent intent=new Intent();
        setResult(result,intent);
        super.onStop();
    }
}