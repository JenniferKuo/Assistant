package com.penguin.assistant;

import java.net.Socket;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Toast;
import android.widget.Button;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    static String host = "59.127.92.148";
    static int port = 8888;

    private Socket socket = null;
    private OutputStream os = null;
    private DataInputStream input = null;
    private DataOutputStream output = null;

    private File folder = null;
    private Button button = null;
    private MediaRecorder myAudioRecorder = null;
    private String outputFile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Initial Variable */
        button = findViewById(R.id.button);
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/assistant/recording.3gp";
        folder = new File(Environment.getExternalStorageDirectory() + File.separator + "assistant");

        /* Connet to Server */
        Thread thread = new Thread(Connection);
        thread.start();

        /* Create Folder */
        if (!folder.exists()) folder.mkdirs();

        /* Button Event */
        button.setOnTouchListener(new Button.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        myAudioRecorder = new MediaRecorder();
                        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                        myAudioRecorder.setOutputFile(outputFile);

                        myAudioRecorder.prepare();
                        myAudioRecorder.start();
                    } catch (IllegalStateException | IOException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_SHORT).show();
                    return true;
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    myAudioRecorder.stop();
                    myAudioRecorder.reset();
                    myAudioRecorder.release();
                    myAudioRecorder = null;

                    MediaPlayer mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(outputFile);
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Thread thread = new Thread(Request);
                        thread.start();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private Runnable Connection = new Runnable() {
        @Override
        public void run() {
            try {
                socket = new Socket(host, port);
                os = socket.getOutputStream();
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.out.println("Connect to Server Successfully");
            }
        }
    };

    private Runnable Request = new Runnable() {
        @Override
        public void run() {
            File file = null;
            BufferedInputStream bis = null;

            try {
                file = new File(folder, "recording.3gp");
                byte[] buff = new byte[(int)file.length()];

                bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(buff,0, buff.length);

                int size = (int)file.length();
                System.out.println(size);
                output.writeInt(size);
                os.write(buff,0, (int)file.length());
                os.flush();
                System.out.println("Done");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(bis != null) bis.close();
                    if(file != null && file.exists()) file.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if (this.os != null) this.os.close();
            if (this.input != null) this.input.close();
            if (this.output != null) this.output.close();
            if (this.socket != null) this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Close Connection Safely");
        }
    }
}
