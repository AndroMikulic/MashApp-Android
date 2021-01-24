package rumpledcode.mashapp;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements RewardedVideoAdListener {

    public RelativeLayout loadingIndicator;
    public ConstraintLayout mainLayout;
    public ListView songList;
    public TextView appTitle;
    public TextView votesLeft;
    public TextView searchBox;
    public TextView curPlaying;
    public ImageView playingIcon;
    public ImageView appIcon;
    public ImageView searchIcon;
    public ImageView appIconLoading;
    public ServerQuery serverQuery;
    public Button retryButton;
    public ConstraintLayout errorLayout;

    final int VOTE_REFRESH_INTERVAL = 2; // in hours
    final int MAXIMUM_VOTE_COUNT = 3;
    final String votesFile = "v.dat";
    int voteCount;
    Date lastFileTime;
    SimpleDateFormat dateFormatGmt;

    private RewardedVideoAd mRewardedVideoAd;

    final String ADS_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    final String ADS_AD_ID = "ca-app-pub-3940256099942544/5224354917";

    /*
    final String ADS_APP_ID = "ca-app-pub-2846368323100628~2472323478";
    final String ADS_AD_ID = "ca-app-pub-2846368323100628/5066697226";
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadingIndicator = findViewById(R.id.loadingPanel);
        mainLayout = findViewById(R.id.mainLayout);
        songList = findViewById(R.id.songListId);
        appTitle = findViewById(R.id.appTitle);
        searchBox = findViewById(R.id.searchSong);
        searchIcon = findViewById(R.id.searchIcon);
        retryButton = findViewById(R.id.retryButton);
        curPlaying = findViewById(R.id.playing);
        playingIcon = findViewById(R.id.playIcon);
        appIcon = findViewById(R.id.appIcon);
        appIconLoading = findViewById(R.id.appIconLoading);
        votesLeft = findViewById(R.id.votesLeft);
        serverQuery = new ServerQuery();

        retryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                errorLayout.setVisibility(View.GONE);
                loadingIndicator.setVisibility(View.VISIBLE);
                appIconLoading.setVisibility(View.VISIBLE);
                TryConnection();
            }
        });

        searchBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    searchBox.setText("");
                }
            }
        });

        appIcon.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, InfoActivity.class));
            }
        });

        curPlaying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlayingSongMenu.class);
                intent.putExtra("SONG", curPlaying.getText());
                songList.setAlpha(0.1f);
                serverQuery.blockRequests = true;
                startActivityForResult(intent, 1);
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                serverQuery.arrayAdapter.getFilter().filter(searchBox.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        LoadVoteFile();
        serverQuery.act = this;
        serverQuery.songList = this.songList;
        TryConnection();

        MobileAds.initialize(this, ADS_APP_ID);
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);
        mRewardedVideoAd.loadAd(ADS_AD_ID, new AdRequest.Builder().build());
    }

    private void LoadVoteFile() {
        dateFormatGmt = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss");
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            FileInputStream inputStream = openFileInput(votesFile);
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                line = bufferedReader.readLine();
                voteCount = Integer.parseInt(line);
                if (voteCount > MAXIMUM_VOTE_COUNT) {
                    voteCount = MAXIMUM_VOTE_COUNT;
                }
                line = bufferedReader.readLine();
                lastFileTime = dateFormatGmt.parse(line);
                Date currentTime = new Date();
                long timeDif = currentTime.getTime() - lastFileTime.getTime();
                timeDif = TimeDifToHours(timeDif);
                if (timeDif >= VOTE_REFRESH_INTERVAL) {
                    voteCount = MAXIMUM_VOTE_COUNT;
                }
                inputStream.close();
            }
            votesLeft.setText("" + voteCount);
        } catch (Exception e) {
            SetMaximumVotes();
        }
        votesLeft.setText("" + voteCount);
        new Thread(new Runnable() {
            @Override
            public void run() {
                VoteRefreshChecker();
            }
        }).start();
    }

    private void TryConnection() {
        if (WifiOnAndConnected() == true) {
            serverQuery.SetUp();
        } else {
            errorLayout = findViewById(R.id.errorMessageLayout);
            loadingIndicator.setVisibility(View.GONE);
            appIconLoading.setVisibility(View.GONE);
            errorLayout.setVisibility(View.VISIBLE);
        }
    }

    private boolean WifiOnAndConnected() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            if (wifiInfo.getNetworkId() == -1) {
                return false; // Not connected to an access point
            }
            return true; // Connected to an access point
        } else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    public void SongInQueue() {
        startActivityForResult(new Intent(MainActivity.this, SongInQueuePopUp.class), 1);
    }

    public void SongAdded() {
        startActivityForResult(new Intent(MainActivity.this, SongAdded.class), 1);
    }

    public void LuckySongAdded() {
        startActivityForResult(new Intent(MainActivity.this, LuckySongAdded.class), 1);
    }

    public void OutOfVotes() {
        Intent intent = new Intent(MainActivity.this, OutOfVotes.class);
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            serverQuery.blockRequests = false;
            loadingIndicator.setVisibility(View.GONE);
            appIconLoading.setVisibility(View.GONE);
            songList.setAlpha(1.0f);
        }
        if (requestCode == 2) {
            Log.w("MY-FILTER", "zatrazen video");
            if (resultCode == 1) {
                Log.w("MY-FILTER", "" + mRewardedVideoAd.isLoaded());
                if (mRewardedVideoAd.isLoaded()) {
                    mRewardedVideoAd.show();
                }
            }
            serverQuery.blockRequests = false;
            loadingIndicator.setVisibility(View.GONE);
            appIconLoading.setVisibility(View.GONE);
            songList.setAlpha(1.0f);

        }
    }

    private void VoteRefreshChecker() {
        Date currentTime;
        long timeDif;
        int i = 0;
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            currentTime = new Date();
            timeDif = currentTime.getTime() - lastFileTime.getTime();
            timeDif = TimeDifToHours(timeDif);
            i++;
            if (timeDif >= VOTE_REFRESH_INTERVAL) {
                i = 0;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SetMaximumVotes();
                    }
                });
            }
        }
    }

    private void SetMaximumVotes() {
        try {
            FileOutputStream outputStream = openFileOutput(votesFile, Context.MODE_PRIVATE);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            lastFileTime = new Date();
            String localTimeInGMT = dateFormatGmt.format(lastFileTime);
            voteCount = MAXIMUM_VOTE_COUNT;
            outputStreamWriter.write(MAXIMUM_VOTE_COUNT + "\n" + localTimeInGMT);
            outputStreamWriter.close();
            votesLeft.setText("" + voteCount);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private long TimeDifToHours(long milisDif) {
        long hoursDif;
        int milisInSecond = 1000;
        int secondsInMinute = 60;
        int minutesInHour = 60;
        hoursDif = milisDif / (milisInSecond * secondsInMinute * minutesInHour);
        return hoursDif;
    }

    @Override
    public void onRewardedVideoAdLoaded() {

    }

    @Override
    public void onRewardedVideoAdOpened() {

    }

    @Override
    public void onRewardedVideoStarted() {

    }

    @Override
    public void onRewardedVideoAdClosed() {
        mRewardedVideoAd.loadAd(ADS_AD_ID, new AdRequest.Builder().build());
    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        SetMaximumVotes();
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {

    }
}
