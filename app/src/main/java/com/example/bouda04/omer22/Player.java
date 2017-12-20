package com.example.bouda04.omer22;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Player extends Activity implements View.OnClickListener {
    private  ArrayList<File> mysongs;
    private MediaPlayer mp;

    private ImageButton ibPlay,ibNxt,ibPv;
    private int pos;
    private int totalDuration;
    private Uri uri;
    private Sensor gyroscopeSensor;
    private SensorEventListener gyroscopeEventListner;
    private SensorManager sensorManager;
    private Thread updateSeekBar;
    private TextView tv;
    private SeekBar SeekBar;
    private Object mPauseLock;
    boolean mpPlaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ibPlay=(ImageButton)findViewById(R.id.play);
        ibNxt=(ImageButton)findViewById(R.id.next);
        ibPv=(ImageButton)findViewById(R.id.previous);
        SeekBar=(SeekBar) findViewById(R.id.SeekBar);
        tv = (TextView)findViewById(R.id.tv);
        ibPlay.setOnClickListener(this);
        ibNxt.setOnClickListener(this);
        ibPv.setOnClickListener(this);
        pos=0;
        mPauseLock = new Object();
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        gyroscopeSensor= sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

       updateSeekBar=new Thread(){
            @Override
            public void run() {
                try{
                        while (true){
                            synchronized (mPauseLock){
                                while (!mpPlaying)
                                        mPauseLock.wait();
                            }
                            SeekBar.setProgress(mp.getCurrentPosition());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        };
        SeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar circularSeekBar, int progress, boolean fromUser) {
                // TODO Insert your code here
                tv.setText(Utility.convertDuration(mp.getCurrentPosition()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mp.seekTo(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
        });
        //this line check if there is gyroscope sensor on your phone
        if(gyroscopeSensor==null){
            Toast.makeText(this,"no gyro",Toast.LENGTH_SHORT).show();
            finish();
        }

        //listener check if there was use of the gyro sensor
        gyroscopeEventListner=new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //if the phone moved on the axis y the player next the current song
                if (sensorEvent.values[1] > 8f) {
                    mp.stop();
 //                   mp.release();
                    pos=(pos+1)%mysongs.size();
                    uri= Uri.parse(mysongs.get(pos).toString());
                    changeSong(uri);
                }
                else if(sensorEvent.values[1]< -8f){
                    mp.stop();
 //                   mp.release();
                    pos=(pos+1)%mysongs.size();
                    uri= Uri.parse(mysongs.get(pos).toString());
                    changeSong(uri);
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {}
        };

        if(mp!=null){
            mp.stop();
            mp.reset();
            mp.release();
        }


        Intent intent=getIntent();
        Bundle b=intent.getExtras();
        mysongs= (ArrayList) b.getParcelableArrayList("songlist");
        pos=b.getInt("pos",0);

        Uri uri= Uri.parse(mysongs.get(pos).toString());
        mp = new MediaPlayer();
        try {
            mp.setDataSource(getApplicationContext(),uri);
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //mp=MediaPlayer.create(getApplicationContext(),uri);

//        mp.start();
 //       SeekBar.setMax(mp.getDuration());
        updateSeekBar.start();

        //mp3 will be started after completion of preparing...
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {

                player.start();
                totalDuration=player.getDuration();
                SeekBar.setMax(totalDuration);
                synchronized (mPauseLock){
                    mpPlaying = true;
                    mPauseLock.notifyAll();
                }

            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {

                pos=(pos+1)%mysongs.size();
                Uri uri= Uri.parse(mysongs.get(pos).toString());
                changeSong(uri);
            }
        });
    }

    private void changeSong(Uri uri){
        try {
            synchronized (mPauseLock){
                mpPlaying = false;
            }
            mp.reset();
            mp.setDataSource(getApplicationContext(),uri);
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
   // @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View view) {
        int id= view.getId();
        switch (id)
        {
            case R.id.play:
                if(mp.isPlaying()){
                    mp.pause();
                    ibPlay.setImageResource(R.mipmap.playl);
                }
                else {
                    mp.start();
                    ibPlay.setImageResource(R.mipmap.pausel);
                }
            break;

            case R.id.next:
                mp.stop();
 //               mp.release();
                pos=(pos+1)%mysongs.size();
                uri= Uri.parse(mysongs.get(pos).toString());
                changeSong(uri);
            break;

            case R.id.previous:
                mp.stop();
 //               mp.release();
                if(pos-1<0){
                    pos=mysongs.size()-1;
                }
                else {
                    pos=pos-1;
                }
                uri= Uri.parse(mysongs.get(pos).toString());
                changeSong(uri);
               // mp=MediaPlayer.create(getApplicationContext(),uri);
              //  mp.start();
               // SeekBar.setMax(mp.getDuration());
            break;
        }




    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(gyroscopeEventListner,gyroscopeSensor,sensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
