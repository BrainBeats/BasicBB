package com.example.brainbeats.basicbb;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.emotiv.insight.IEmoStateDLL;
//import com.google.android.gms.appindexing.Action;
//import com.google.android.gms.appindexing.AppIndex;
//import com.google.android.gms.common.api.GoogleApiClient;

import com.emotiv.insight.IEdk;
import com.emotiv.insight.IEmoStateDLL;
import com.emotiv.insight.IEdk.*;
import com.emotiv.insight.IEdk.IEE_DataChannel_t;
import com.emotiv.insight.IEdk.IEE_Event_t;
import com.emotiv.insight.IEdkErrorCode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.*;

public class HeadsetCalibration extends Activity {
    private MediaPlayer mpNeutre;
    private MediaPlayer mpHard;
    private MediaPlayer mpCalm;
    private int MpQuantity = 4;

    private boolean isEnablGetData = false;
    private boolean isEnableWriteFile = false;
    private boolean launched = false;
    private long time;
    private int MpNumber = 0;
    int userId;
    private boolean lock = false;
    Timer timer = new Timer();

    private int[] array;
    private int[] array2;

    private String[] filenames = new String[MpQuantity];
    private BufferedWriter[] motion_writer = new BufferedWriter[MpQuantity];

    private Chronometer chronometer;
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;


    IEE_DataChannel_t[] Channel_list = {IEE_DataChannel_t.IED_AF3, IEE_DataChannel_t.IED_T7, IEE_DataChannel_t.IED_Pz,
            IEE_DataChannel_t.IED_T8, IEE_DataChannel_t.IED_AF4,IEE_DataChannel_t.IED_RAW_CQ,IEE_DataChannel_t.IED_COUNTER};
    String[] Name_Channel = {"AF3", "T7", "Pz", "T8", "AF4"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration_layout);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        instantiateFileNames();

        chronometer = (Chronometer) findViewById(R.id.chronometer);
        Button Start_button = (Button) this.findViewById(R.id.default_activity_button);
        Button Stop_button = (Button) this.findViewById(R.id.button);
        IEdk.IEE_EngineConnect(this, "");
        Thread processingThread = new Thread() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                super.run();
                while (true) {
                    try {
                        handler.sendEmptyMessage(0);
                        handler.sendEmptyMessage(1);
                        if (isEnablGetData && isEnableWriteFile) handler.sendEmptyMessage(2);
                        array = IEmoStateDLL.IS_GetContactQualityFromAllChannels();
                        //    Log.e("total", String.valueOf(IEmoStateDLL.IS_GetContactQualityFromAllChannels()));
                        Log.e("etat AF3 ", String.valueOf(array[3]));
                        //                    Log.e("etat T7", String.valueOf(array[7]));
                        //                  Log.e("etat Pz ", String.valueOf(array[9]));
                        //                Log.e("etat T8 ", String.valueOf(array[12]));
                        //              Log.e("etat AF4 ", String.valueOf(array[16]));
                        //            //Log.e("etat 7 ", String.valueOf(array.length));
                        //          Log.e("Mpnumber ", String.valueOf(MpNumber));
                        //           if(MpNumber != 0 && MpNumber<=4) {
                        //             Log.e("filename ", String.valueOf(filenames[MpNumber - 1]));
                        Log.e("Counter :", String.valueOf(IEdk.IEE_GetAverageBandPowers(Channel_list[Channel_list.length-1])));

                        Log.e("channel :", String.valueOf(Channel_list[Channel_list.length-1]));

                        Thread.sleep(200);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        processingThread.start();

        Start_button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mpNeutre = MediaPlayer.create(arg0.getContext(), R.raw.punk);
                mpHard = MediaPlayer.create(arg0.getContext(), R.raw.hard);
                mpCalm = MediaPlayer.create(arg0.getContext(), R.raw.calm);

                Log.e("FFTSample", "Start Write File");
                setDataFile();
                isEnableWriteFile = true;
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                // task to be done every 20000 milliseconds
                                time = showElapsedTime();
                                PlayMv();
                                changeMpNumber();
                            }
                        });
                    }
                }, 0, 30000);
            }
        });

        Stop_button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Log.e("FFTSample", "Stop Write File");
                StopWriteFile();
                isEnableWriteFile = false;
            }
        });
    }

    private void PlayMv() {
        if (MpNumber % 2 == 0) {
            //mpNeutre.start();
        } else {
            switch (MpNumber) {
                case 1:
                    mpCalm.start();
                    break;
                case 3:
                    mpHard.start();
                    break;
            }
        }
    }

    private void changeMpNumber() {
        MpNumber++;
    }

    private long showElapsedTime() {
        long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
        Toast.makeText(HeadsetCalibration.this, "Elapsed milliseconds: " + elapsedMillis,
                Toast.LENGTH_SHORT).show();
        return elapsedMillis;
    }

    private void instantiateFileNames(){
        for (int i=0; i<MpQuantity-1; i++){
            if (i%2 == 0 ){
                filenames[i] = "_Neutre";
            }
            filenames[1]="_Calm";
            filenames[3]="_Hard";
        }
    }

    private void setDataFile() {
        try {
            String eeg_header = "Channel , Theta ,Alpha ,Low beta ,High beta , Gamma ";
            File root = Environment.getExternalStorageDirectory();
            String file_path = root.getAbsolutePath() + "/FFTSample/";
            File folder = new File(file_path);
            if (!folder.exists()) {
                Log.e("camarche po", "on va creer");
                folder.mkdirs();
            }
            for (int i = 0; i < filenames.length; i++) {
                motion_writer[i] = new BufferedWriter(new FileWriter(file_path + "bandpowerValue"+ filenames[i]+".csv"));
                motion_writer[i].write(eeg_header);
                motion_writer[i].newLine();
            }
        }catch(Exception e){
            Log.e("", "Exception" + e.getMessage());
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            View textView = findViewById(R.id.textView);
            View textView2 = findViewById(R.id.textView2);

            switch (msg.what) {
                case 0:
                    int state = IEdk.IEE_EngineGetNextEvent();
                    if (state == IEdkErrorCode.EDK_OK.ToInt()) {
                        int eventType = IEdk.IEE_EmoEngineEventGetType();
                        userId = IEdk.IEE_EmoEngineEventGetUserId();
                        if (eventType == IEE_Event_t.IEE_UserAdded.ToInt()) {
                            Log.e("SDK", "User added");
                            textView.setVisibility(View.VISIBLE);
                            IEdk.IEE_FFTSetWindowingType(userId, IEdk.IEE_WindowsType_t.IEE_BLACKMAN);
                            isEnablGetData = true;
                        }
                        if (eventType == IEE_Event_t.IEE_UserRemoved.ToInt()) {
                            textView.setAlpha(0.0f);
                            Log.e("SDK", "User removed");
                            isEnablGetData = false;
                        }
                    }
                    break;
                case 1:
                    int number = IEdk.IEE_GetInsightDeviceCount();
                    if (number != 0) {
                        if (!lock) {
                            lock = true;
                            IEdk.IEE_ConnectInsightDevice(0);
                        }
                    } else lock = false;
                    break;
                case 2:
                    textView2.setVisibility(View.VISIBLE);
                    for (int i = 0; i < Channel_list.length-2; i++) {
                        double[] data = IEdk.IEE_GetAverageBandPowers(Channel_list[i]);

                        if (data.length == 5) {
                            try {
                                if (MpNumber !=0 && MpNumber<MpQuantity+1){
                                    motion_writer[MpNumber-1].write(Name_Channel[i]);
                                    for (int j = 0; j < data.length; j++)
                                        addData(data[j]);
                                    if(MpNumber != 0 && MpNumber<=4) {
                                        Log.e("writing in", String.valueOf(filenames[MpNumber - 1]));
                                    }
                                    motion_writer[MpNumber-1].newLine();}
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
            }

        }
    };

    private void StopWriteFile() {
        try {
            View textView2 = findViewById(R.id.textView2);
            for (int i = 0; i < filenames.length; i++) {
                motion_writer[i].flush();
                motion_writer[i].close();
                textView2.setAlpha(0.0f);
                chronometer.stop();
                mpCalm.stop();
                mpHard.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addData(double data) {
        if (motion_writer == null) {
            return;
        }

        String input = "";
        input += ("," + String.valueOf(data));
        try {
            motion_writer[MpNumber-1].write(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
