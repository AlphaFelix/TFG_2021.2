package com.example.myapplication;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;



import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

public class MainActivity extends AppCompatActivity {

    public final static String[] MODULE_MAC = {"00:12:12:04:22:25","7C:9E:BD:F3:CC:46"};
    //public final static String MODULE_MAC = "7C:9E:BD:F3:CC:46"; //Esp
    public final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    BluetoothAdapter bta;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    ConnectedThread btt = null;
    TextView response;
    int userFloor = 0;
    TextView favoriteFloor;

    public Handler mHandler;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Recover favorite floor
        SharedPreferences mPrefs = getSharedPreferences("user", MODE_PRIVATE);
        userFloor = mPrefs.getInt("userF", 0);

        Log.i("[BLUETOOTH]", "Creating listeners");

        favoriteFloor = (Button) findViewById(R.id.floorFav);
        favoriteFloor.setText("Andar favorito: " + userFloor);
        favoriteFloor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("[BLUETOOTH]", "Attempting to send data");
                if (mmSocket.isConnected() && btt != null) { //if we have connection to the bluetooth module
                    String sendTxt = Integer.toString(userFloor);
                    btt.write(sendTxt.getBytes());

                } else {
                    Toast.makeText(MainActivity.this, "Fora do alcance", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*
        button1 = (Button) findViewById(R.id.floor1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("[BLUETOOTH]", "Attempting to send data");
                if (mmSocket.isConnected() && btt != null) { //if we have connection to the bluetooth module
                    String sendTxt = "1";
                    btt.write(sendTxt.getBytes());

                    //disable the button and wait for 10 seconds to enable it again
                    button1.setEnabled(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                Thread.sleep(10000);
                            }catch(InterruptedException e){
                                return;
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    button1.setEnabled(true);
                                }
                            });

                        }
                    }).start();

                } else {
                    Toast.makeText(MainActivity.this, "Fora do alcance", Toast.LENGTH_SHORT).show();
                }
            }
        });*/

        bta = BluetoothAdapter.getDefaultAdapter();

        //if bluetooth is not enabled then create Intent for user to turn it on
        if (!bta.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        } else {
            initiateBluetoothProcess();
        }
    }


    // Bluetooth
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT){
            initiateBluetoothProcess();
        }
    }

    public void button(int floor) {
        Log.i("[BLUETOOTH]", "Attempting to send data");
        if (mmSocket.isConnected() && btt != null) { //if we have connection to the bluetooth module
            String sendTxt = Integer.toString(floor);
            btt.write(sendTxt.getBytes());
        } else {
            Toast.makeText(MainActivity.this, "Fora do alcance", Toast.LENGTH_SHORT).show();
        }
    }

    public void button_fav(int floor) {
        userFloor = floor;
        //Save on memory
        SharedPreferences mPrefs = getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putInt("userF", userFloor).apply();
        favoriteFloor.setText("Andar favorito: " + userFloor);
    }

    public void initiateBluetoothProcess(){

        if(bta.isEnabled()){
            //attempt to connect to bluetooth module
            BluetoothSocket tmp;
            int deviceId = 0;
            boolean success = false;

            while(deviceId < MODULE_MAC.length) {
                mmDevice = bta.getRemoteDevice(MODULE_MAC[deviceId]);
                //create socket
                try {
                    tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    mmSocket = tmp;
                    mmSocket.connect();
                    Log.i("[BLUETOOTH]","Connected to: "+mmDevice.getName());
                    success = true;
                }catch(IOException e){
                    try{mmSocket.close();}catch(IOException c){return;}
                    deviceId++;
                }
                finally {
                    if(success) {
                        break;
                    }
                }
            }

            Log.i("[BLUETOOTH]", "Creating handler");
            mHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message msg) {
                    if(msg.what == ConnectedThread.RESPONSE_MESSAGE){
                        String txt = (String)msg.obj;
                        if(response.getText().toString().length() >= 30){
                            response.setText("");
                            response.append(txt);
                        }else{
                            response.append("\n" + txt);
                        }
                    }
                }
            };

            Log.i("[BLUETOOTH]", "Creating and running Thread");
            btt = new ConnectedThread(mmSocket,mHandler);
            btt.start();
        }
    }
}