package com.example.brainbeats.basicbb;


import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.widget.Toast;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songPosn;
    private boolean shuffle=false;
    private boolean brainMode=false;
    private Random rand;

    private String songTitle="";
    private static final int NOTIFY_ID=1;
    //inner binder class
    private final IBinder musicBind = new MusicBinder();

    private BrainmodeService BMSrv;
    private Intent recordIntent;
    Timer timer = new Timer();

    public void onCreate(){
        //create the service
        super.onCreate();
        //initialize position
        songPosn=0;
        //create player
        player = new MediaPlayer();

        rand=new Random();
        initMusicPlayer();
    }

    public void callUpdateInMain() {
        Intent intent = new Intent("update");
        intent.putExtra("message", "update");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    public void setList(ArrayList<Song> theSongs){
        songs=theSongs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void initMusicPlayer(){
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }


    public void playSong(){
        player.reset();
        //get song
        Song playSong = songs.get(songPosn);
        songTitle=playSong.getTitle();
        //get id
        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        } catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
        callUpdateInMain();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        stopService(recordIntent);
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition()>0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopService(recordIntent);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
        .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    public void setSong(int songIndex){
        songPosn=songIndex;
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getPosition(){
        return songPosn;
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    public void playPrev(){
        songPosn--;
        if(songPosn < 0) songPosn=songs.size()-1;
        playSong();
    }

    public void playNext(){
        if (brainMode) {
           pausePlayer();
           brainModeRecord(new Handler.Callback() {
               /**
                * this executed after tag is found
                * @param message contiens tag
                */
               @Override
               public boolean handleMessage(Message message) {
                   String actualState;
                   if (message.arg1 == 1) {
                       actualState = getString(R.string.state_calm);
                   } else {
                       actualState = getString(R.string.state_excited);
                   }
                   Log.e("Emotion","found");
                   System.out.println("Looking for a " + actualState+" music :)");
                   ArrayList <Integer> arrayWithTheGoodState = stateFilter(songs, actualState);
                   if (arrayWithTheGoodState.size() > 0) {
                       songPosn = arrayWithTheGoodState.get(rand.nextInt(arrayWithTheGoodState.size()));
                       System.out.println("New music set");
                   } else {
                       int newSong = songPosn;
                       while (newSong == songPosn) {
                           newSong = rand.nextInt(songs.size());
                       }
                       songPosn = newSong;
                   }
                   playSong();
                   return true;
               }
           });
        } else {
            if (shuffle) {
                int newSong = songPosn;
                while (newSong == songPosn) {
                    newSong = rand.nextInt(songs.size());
                }
                songPosn = newSong;
            } else {
                songPosn++;
                if (songPosn >= songs.size()) songPosn = 0;
            }
            playSong();
        }



    }

    public void setShuffle(){
        if(shuffle) {
            shuffle=false;
            Log.e("SHUFFLE","OFF");
        } else {
            shuffle = true;
            Log.e("SHUFFLE", "ON");
        }
    }


    public void setBrainMode(){
        if(brainMode) {
            brainMode=false;
        } else {
            brainMode = true;
            BMModestart();
        }
    }

    private void BMModestart() {
        if (recordIntent == null) {
            recordIntent = new Intent(this, BrainmodeService.class);
            bindService(recordIntent, BMConnection, Context.BIND_AUTO_CREATE);
            startService(recordIntent);
        }
    }

    //connect to the BMservice
    private ServiceConnection BMConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BrainmodeService.BrainmodeBinder binder = (BrainmodeService.BrainmodeBinder)service;
            //get service
            BMSrv = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            BMSrv.stopBrainMode();
        }
    };


    private void brainModeRecord(final Handler.Callback callback) {
        BMSrv.startWrite();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                BMSrv.stopWrite();
                System.out.println("SEND FILE TO SERVER");
                String filepath = BMSrv.getFilePath() + BMSrv.FILENAME;
                final String BASE_URL = "http://brainbeats.cleverapps.io/";
                final OkHttpClient client = new OkHttpClient();
                final File hardFile = new File(filepath);
                final Request testrequest = createUploadfileRequest(hardFile, BASE_URL + "models/testmodel");
                client.newCall(testrequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String tag = response.body().string();
                        Message message = new Message();
                        if (tag.equals("calm")) {
                            message.arg1 = 1;
                        } else {
                            message.arg1 = 2;
                        }
                        callback.handleMessage(message);
                    }
                });
            }
        }, 10000);
    }

    private ArrayList <Integer> stateFilter (ArrayList <Song> songs, String state) {
        ArrayList <Integer> myArray= new ArrayList<Integer>();
        for (int i=0; i<songs.size();i++) {
            String state1 = songs.get(i).getState();
            if (state1.equals(state))
                myArray.add(i);
        }
        return myArray;
    }

    private Request createUploadfileRequest(File file, String url) {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse("text/csv"), file))
                .build();
        return new Request.Builder()
                .url(url)
                .post(body)
                .build();
    }
}