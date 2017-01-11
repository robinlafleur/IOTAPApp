package se.mah.ae5710.iotapapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    public Thread mConnectThread;
    public Thread mConnectedThread;
    private BluetoothDevice mDevice;
    TextView txtArduino, txtString, txtStringLength, sensorView0, sensorView1, sensorView2, sensorView3, sensorView4, sensorView5;
    double in;
    String sensor0;
    String Reception;
    String[] separated = new String[10000];
    final int handlerState = 0;                         //used to identify handler message
    private StringBuilder recDataString = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.i("No Bluetooth", "No bluetooth");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mDevice = device;
                Log.i("Connected", "Connected");
            }
            mConnectThread = new ConnectThread(mDevice);
            mConnectThread.start();
            sensorView0 = (TextView) findViewById(R.id.sensorView0);
            sensorView1 = (TextView) findViewById(R.id.sensorView1);
            sensorView2 = (TextView) findViewById(R.id.sensorView2);
            sensorView3 = (TextView) findViewById(R.id.sensorView3);
            sensorView4 = (TextView) findViewById(R.id.sensorView4);
            sensorView5 = (TextView) findViewById(R.id.sensorView5);
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                new ConnectedThread(mmSocket).start();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {

            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;

            while (true) {
                InputStream inputStream;
                try {
                    inputStream = mmSocket.getInputStream();
                    byte[] buffer2 = new byte[256];
                    if (inputStream.available() > 0) {
                        inputStream.read(buffer2);
                        for (int i = 0; i < buffer2.length && buffer2[i] != 0; i++) {
                            String strInput = new String(buffer2, 0, i);
                            Reception = strInput;
                            separated = Reception.split(",");
                        }
                        try {
                            sleep(1000);
                        } catch (InterruptedException ie) {
                        }

                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Reception != null) {
                                boolean completeSequence = true;
                                for (int i = 0; i > separated.length; i++) {
                                    if (separated[i] == null) {
                                        completeSequence = false;
                                    }
                                }
                                if (completeSequence) {
//                              sensorView0.setText("X value = " + Reception);
                                    sensorView0.setText("X value = " + separated[1]);
                                    sensorView1.setText("Y value = " + separated[2]);
                                    sensorView2.setText("Z value = " + separated[3]);
                                    sensorView3.setText("dX value = " + separated[4]);
                                    sensorView4.setText("dY value = " + separated[5]);
                                    sensorView5.setText("dZ value = " + separated[6]);

                                } else {
                                    Log.i("Error", "Uncomplete Sequence");
                                }
//
                            } else {
                                Log.i("Error", "Reception empty");
                            }
                        }
                    });
                    ;
                    for (int i = begin; i < bytes; i++) {
                        if (buffer[i] == "#".getBytes()[0]) {
                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if (i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {

            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = msg.arg1;
            int end = msg.arg2;
            switch (msg.what) {
                case 1:
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    break;
            }
        }

        ;
    };

}