package com.example.brainbeats.basicbb;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brainbeats.basicbb.data.Tagger;
import com.example.brainbeats.basicbb.settings.PreferenceWithHeaders;


public class MainActivity extends AppCompatActivity implements MediaPlayerControl, Serializable{

    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private MusicController controller;
    private boolean paused=false, playbackPaused=false, brainMode=false;
    private Tagger tagger;
    private View currentPosition;
    public TextView stateDisplay;
    public TextView musicDisplay;
    public TextView artistDisplay;
    public LinearLayout layout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        tagger = new Tagger(this);

        //Récupère les chansons
        getSongList();

        //Tri les chansons par ordre alphabétouque
        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        setController();
        musicDisplay = (TextView) findViewById(R.id.musicDisplay);
        stateDisplay= (TextView) findViewById(R.id.stateDisplay);
        artistDisplay= (TextView) findViewById(R.id.artistDisplay);
        layout =(LinearLayout)findViewById(R.id.laylay);
        //Récupérer le tag de position qui lancera la bonne musique au toucher
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDisplay();
        }
    };

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if (playbackPaused) {
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    public void updateDisplay() {
        stateDisplay.setText(songList.get(musicSrv.getPosition()).getState());
        musicDisplay.setText(songList.get(musicSrv.getPosition()).getTitle());
        artistDisplay.setText(songList.get(musicSrv.getPosition()).getArtist());
    }

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            int i=0;
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);

                songList.add(new Song(thisId, thisTitle, thisArtist, getTag(thisId)));
            }
            while (musicCursor.moveToNext());
        }
    }

    private String getTag(long thisId) {
        final String tagBySongID = tagger.getTagBySongID(thisId);
        System.out.println(tagBySongID);
        return tagBySongID;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                toaster("Shuffle switched");
                break;
            case R.id.brain_mode:
                if(brainMode) {
                    brainMode=false;
                    musicSrv.setBrainMode();
                    layout.setBackground(getResources().getDrawable(R.drawable.background_display));
                    item.setIcon(getResources().getDrawable(R.drawable.ic_bb_off_36dp));
                    Log.e("BRAINBEATS MODE","OFF");
                    toaster("BrainMode OFF");
                } else {
                    brainMode = true;
                    Log.e("BRAINBEATS MODE", "ON");
                    musicSrv.setBrainMode();
                    layout.setBackground(getResources().getDrawable(R.drawable.background_display_on));
                    item.setIcon(getResources().getDrawable(R.drawable.ic_bb_on_36dp));
                    toaster("BrainMode ON");
                }
                break;
            case R.id.headset:
                Intent intent_fft = new Intent(MainActivity.this, HeadsetService.class);
                startActivity(intent_fft);
                break;
            case R.id.calibration:
                Intent intent_calibration = new Intent(MainActivity.this, HeadsetCalibration.class);
                startActivity(intent_calibration);
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
            case R.id.settings:
                final Intent settingsIntent = new Intent(this, PreferenceWithHeaders.class);
                startActivity(settingsIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver,
                new IntentFilter("update"));
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    private void setController(){
        //set the controller up
        controller = new MusicController(this);

        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    @Override
    public void start() {
        musicSrv.go();
        toaster("Start Music");
        try {
            Thread.sleep(500,0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateDisplay();
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
        toaster("Pause");
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
        return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    //play next
    private void playNext(){
        if (brainMode) {
            toaster("Brain Record Start. Next song in 10s", true);
        } else {
            toaster("Next Song");
        }
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
        try {
            Thread.sleep(500,0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //play previous
    private void playPrev(){
        musicSrv.playPrev();
        toaster("Previous Song");
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
        try {
            Thread.sleep(500,0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void toaster (String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void toaster (final String message, boolean isLong) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }


}