package com.example.brainbeats.basicbb;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.app.Activity;
import android.widget.Toast;


import com.emotiv.insight.IEdk;
import com.emotiv.insight.IEdkErrorCode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BrainmodeService extends Service {


    private final IBinder BrainmodeBind = new BrainmodeBinder();

    private Activity RootActivity;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean lock = false;
    private boolean isEnablGetData = false;
    private boolean isEnableWriteFile = false;
    private boolean isRunning = true;
    int userId;
    public final String FILENAME = "bandpowerValue_Transient.csv";

    private BufferedWriter motion_writer;
    IEdk.IEE_DataChannel_t[] Channel_list = {IEdk.IEE_DataChannel_t.IED_AF3, IEdk.IEE_DataChannel_t.IED_T7, IEdk.IEE_DataChannel_t.IED_Pz,
            IEdk.IEE_DataChannel_t.IED_T8, IEdk.IEE_DataChannel_t.IED_AF4};
    String[] Name_Channel = {"AF3","T7","Pz","T8","AF4"};
    private Context context;

    public void onCreate(){
        super.onCreate();

        context = getApplicationContext();

        Log.e("BrainMode service","started");
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //Connect to emoEngine
        IEdk.IEE_EngineConnect(this,"");
       Thread processingThread=new Thread()
        {
            @Override
            public void run() {
                // TODO Auto-generated method stub
            super.run();
            while(isRunning) {
                try {
                    handler.sendEmptyMessage(0);
                    handler.sendEmptyMessage(1);
                    if(isEnablGetData && isEnableWriteFile)handler.sendEmptyMessage(2);

                    Thread.sleep(200);
                }

                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            }
        };
        processingThread.start();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    int state = IEdk.IEE_EngineGetNextEvent();
                    if (state == IEdkErrorCode.EDK_OK.ToInt()) {
                        int eventType = IEdk.IEE_EmoEngineEventGetType();
                        userId = IEdk.IEE_EmoEngineEventGetUserId();
                        if(eventType == IEdk.IEE_Event_t.IEE_UserAdded.ToInt()){
                            Toast.makeText(context, "User added", Toast.LENGTH_SHORT).show();
                            Log.e("SDK","User added");
                            IEdk.IEE_FFTSetWindowingType(userId, IEdk.IEE_WindowsType_t.IEE_BLACKMAN);
                            isEnablGetData = true;
                        }
                        if(eventType == IEdk.IEE_Event_t.IEE_UserRemoved.ToInt()){
                            Log.e("SDK","User removed");
                            isEnablGetData = false;
                        }
                    }

                    break;
                case 1:
                    int number = IEdk.IEE_GetInsightDeviceCount();
                    if(number != 0) {
                        if(!lock){
                            lock = true;
                            IEdk.IEE_ConnectInsightDevice(0);
                        }
                    }
                    else lock = false;
                    break;
                case 2:
                    for(int i=0; i < Channel_list.length; i++) {
                        double[] data = IEdk.IEE_GetAverageBandPowers(Channel_list[i]);
                        if (data != null) {
                            if (data.length == 5) {
                                try {
                                    motion_writer.write(Name_Channel[i]);
                                    for (int j = 0; j < data.length; j++)
                                        addData(data[j]);
                                    motion_writer.newLine();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    break;
            }

        }

    };

    public void addData(double data) {
        if (motion_writer == null) {
            return;
        }
        String input = "";
        input += ("," + String.valueOf(data));
        try {
            motion_writer.write(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
/*
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
*/
    public void stopBrainMode(){
        isRunning = false;
    }

    public boolean getConnected(){
        return isEnablGetData;
    }

    private void setDataFile() {
        try {
            String eeg_header = "Channel , Theta ,Alpha ,Low beta ,High beta , Gamma ";
            String file_path = getFilePath();
            File folder=new File(file_path);
            if(!folder.exists()) {
                folder.mkdirs();
            }
            motion_writer = new BufferedWriter(new FileWriter(file_path+FILENAME));
            motion_writer.write(eeg_header);
            motion_writer.newLine();
        } catch (Exception e) {
            Log.e("","Exception"+ e.getMessage());
        }
    }

    public String getFilePath() {
        File root = Environment.getExternalStorageDirectory();
        return root.getAbsolutePath()+ "/brainbeats/";
    }

    private void StopWriteFile() {
        try {
            motion_writer.flush();
            motion_writer.close();
            isEnableWriteFile = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startWrite(){
        setDataFile();
        isEnableWriteFile = true;
    }

    public void stopWrite(){StopWriteFile();}

    public class BrainmodeBinder extends Binder {
        BrainmodeService getService() {
            return BrainmodeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return BrainmodeBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return false;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
    }
}