package rumpledcode.mashapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Andro on 28.1.2018..
 */

public class ServerQuery {

    private int port = 1337;
    private InetAddress serverIP;

    private String IPTag = "MAIP:";
    private String curPlayingTag = "CURPL:";
    private String REQUEST_LIST = "LIST";
    private String REQUEST_SONG = "SONG:";
    private String IN_QUEUE_ERROR = "ERROR";
    private String SONG_ADDED = "ADDED";
    private String LUCK = "LUCK";
    private int socketTimeout = 1000;

    ListView songList;
    MainActivity act;

    ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> songs = new ArrayList<>();
    private ArrayList<String> songDisplayNames = new ArrayList<>();
    private String selectedSong = "";
    boolean blockRequests = false;


    public void SetUp() {
        songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!blockRequests) {
                    act.songList.setAlpha(0.1f);
                    act.loadingIndicator.setVisibility(View.VISIBLE);
                    act.appIconLoading.setVisibility(View.VISIBLE);
                    selectedSong = songList.getItemAtPosition(position).toString();
                    Log.w("MY-FILTER", selectedSong);
                    new Thread(new Runnable() {
                        public void run() {
                            RequestSong();
                        }
                    }).start();
                }
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                ListenToBroadcast();
            }
        }).start();
    }

    public void ListenToBroadcast() {
        boolean setupDone = false;
        try {
            DatagramSocket udpSocket = new DatagramSocket(port);
            byte[] message = new byte[512];
            DatagramPacket packet = new DatagramPacket(message, message.length);
            while (true) {
                udpSocket.receive(packet);
                final String text = new String(message, 0, packet.getLength());

                if (!setupDone && text.startsWith(IPTag)) {
                    setupDone = true;
                    String IPstr = text.substring(IPTag.length());
                    this.serverIP = InetAddress.getByName(IPstr);
                    GetServerInformation();
                }
                if (text.startsWith(curPlayingTag) && setupDone) {
                    final String temp1 = text.substring(curPlayingTag.length());
                    final String temp2;
                    if(temp1.endsWith(".mp3") || temp1.endsWith(".MP3")) {
                        temp2 = temp1.substring(0, temp1.length() - 4);
                    }else{
                        temp2 = temp1;
                    }
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            act.curPlaying.setText(temp2);
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void RequestSong() {
        try {
            blockRequests = true;
            if(act.voteCount <= 0){
                act.OutOfVotes();
                return;
            }
            Socket socket = new Socket();
            InetSocketAddress serverAddr = new InetSocketAddress(serverIP, port);
            socket.setSoTimeout(socketTimeout);
            socket.connect(serverAddr);
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.write(REQUEST_SONG + selectedSong);
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals(IN_QUEUE_ERROR)) {
                    act.SongInQueue();
                }
                if (message.startsWith(SONG_ADDED)) {
                    if (message.contains(LUCK)) {
                        act.LuckySongAdded();
                    } else {
                        DeductVotePoint();
                        act.SongAdded();
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            RequestSong();
        } catch (SocketException e) {
            try {
                Thread.sleep(socketTimeout);
                RequestSong();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void DeductVotePoint() {
        try {
            act.voteCount--;
            FileOutputStream outputStream = act.openFileOutput(act.votesFile, Context.MODE_PRIVATE);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            if (act.voteCount == act.MAXIMUM_VOTE_COUNT - 1) { //Ako je tek prvi vote potrosen
                Date curTime = new Date();
                String localTimeInGMT = act.dateFormatGmt.format(curTime);
                outputStreamWriter.write(act.voteCount + "\n" + localTimeInGMT);
                act.lastFileTime = curTime;
            }else{
                String localTimeInGMT = act.dateFormatGmt.format(act.lastFileTime);
                outputStreamWriter.write(act.voteCount + "\n" + localTimeInGMT);
            }
            outputStreamWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                act.votesLeft.setText("" + act.voteCount);
            }
        });
    }

    public void GetServerInformation() {
        try {
            Socket socket = new Socket();
            InetSocketAddress serverAddr = new InetSocketAddress(serverIP, port);
            socket.setSoTimeout(socketTimeout);
            socket.connect(serverAddr);
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.write(REQUEST_LIST);
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            final String name = in.readLine();
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    act.playingIcon.setVisibility(View.VISIBLE);
                    act.curPlaying.setVisibility(View.VISIBLE);
                    act.appTitle.setText("MashApp - " + name);
                }
            });
            while ((message = in.readLine()) != null) {
                String _song = message;
                String _songName = message;
                if(_songName.endsWith(".mp3") || _songName.endsWith(".MP3")){
                    _songName = _songName.substring(0, _songName.length() - 4);
                }
                songDisplayNames.add(_songName);
                songs.add(_song);
            }

            FillListView();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    private void FillListView() {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                act.loadingIndicator.setVisibility(View.GONE);
                act.appIconLoading.setVisibility(View.GONE);
                arrayAdapter = new ArrayAdapter<String>(act, android.R.layout.simple_list_item_1, songs) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text = view.findViewById(android.R.id.text1);
                        Typeface typeface = ResourcesCompat.getFont(act, R.font.dosis_book);
                        text.setTypeface(typeface);
                        text.setTextSize(16.0f);
                        text.setHeight(32);
                        text.setMaxHeight(32);
                        text.setTextColor(Color.parseColor("#e6e6e6"));
                        return view;
                    }
                };
                songList.setAdapter(arrayAdapter);
                act.searchIcon.setVisibility(View.VISIBLE);
                act.searchBox.setVisibility(View.VISIBLE);
            }
        });
    }
}
