package com.example.radcityapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

/**
 * thread for connecting to the Pi
 */
public class PiConnectThread extends Thread {
    volatile boolean bRunning = true;
    private BluetoothSocket mmSocket = null;
    private BluetoothDevice mmDevice = null;
    final private Handler mHandler;
    final private Context mContext;
    private InputStream mmInStream;


    public PiConnectThread(Handler handler, Context context) {
        mHandler = handler;
        mContext = context;
    }

    /**
     * find raspberry pi bluetooth device to start sending data to
     * @throws InterruptedException
     */
    private void findDevice() throws InterruptedException {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        while(mmDevice == null){
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (deviceName.equals("raspberrypi")) {
                    mmDevice = device;
                    break;
                }
            }
        }
    }

    /**
     * connect to the pi and start a new thread to service the connection
     */
    public void run() {

        while(bRunning) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();

            try {
                findDevice();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BluetoothSocket tmp = null;

            mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, "Connecting to bike...", Toast.LENGTH_LONG).show();
                }
            });

            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("146e39ac-103d-416b-9b3d-93c1596724c3"));
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
                break;
            }
            mmSocket = tmp;

            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                    break;
                }
            }

            try {
                mmInStream = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
                break;
            }

            byte[] buff = new byte[1024];
            int numBytes;
            mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, "Connected!", Toast.LENGTH_SHORT).show();
                }
            });

            while (bRunning) {
                try {
                    numBytes = mmInStream.read(buff);
                    Message readMsg = mHandler.obtainMessage(
                            1, numBytes, -1,
                            buff);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    mHandler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(mContext, "Input stream disconnected",    Toast.LENGTH_LONG).show();
                        }
                    });
                    try {
                        mmInStream.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }

                    try {
                        mmSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    // wait before trying connection again
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    break;
                }
            }
        }
        try {
            mmInStream.close();
        } catch (IOException e) {
            e.printStackTrace();

        }
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}