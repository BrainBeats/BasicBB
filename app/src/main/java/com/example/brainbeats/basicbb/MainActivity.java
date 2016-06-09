package com.example.brainbeats.basicbb;

import android.content.Intent;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;

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
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
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
                break;
            case R.id.brain_mode:
                if(brainMode) {
                    brainMode=false;
                    musicSrv.setBrainMode();
                    item.setIcon(getResources().getDrawable(R.drawable.ic_bb_off_36dp));
                    Log.e("BRAINBEATS MODE","OFF");
                } else {
                    brainMode = true;
                    Log.e("BRAINBEATS MODE", "ON");
                    musicSrv.setBrainMode();
                    item.setIcon(getResources().getDrawable(R.drawable.ic_bb_on_36dp));
                }
                break;
            case R.id.headset:
                Intent intent = new Intent(MainActivity.this, HeadsetService.class);
                startActivity(intent);
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
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
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
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
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
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    //play previous
    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }
}
