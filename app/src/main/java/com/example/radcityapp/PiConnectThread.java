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
    private BluetoothSocket mSocket = null;
    private BluetoothDevice mDevice = null;
    final private Handler mHandler;
    final private Context mContext;
    private InputStream mInStream;

    /**
     * Constructor
     * @param handler
     * @param context
     */
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
        while(mDevice == null){
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                // hard coded for now, could be made dynamic in the future
                if (deviceName.equals("raspberrypi")) {
                    mDevice = device;
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
            BluetoothSocket tmpSoc = null;

            mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, "Connecting to bike...", Toast.LENGTH_LONG).show();
                }
            });

            // create a socket using the service UUID
            try {
                tmpSoc = mDevice.createRfcommSocketToServiceRecord(UUID.fromString("146e39ac-103d-416b-9b3d-93c1596724c3"));
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
                break;
            }
            mSocket = tmpSoc;

            // connect to the socket
            try {
                mSocket.connect();
            } catch (IOException connectException) {
                try {
                    mSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                    break;
                }
            }

            // establish an input stream
            try {
                mInStream = mSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
                break;
            }

            // init a buffer for reading
            byte[] buff = new byte[1024];
            int numBytes;
            mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, "Connected!", Toast.LENGTH_SHORT).show();
                }
            });

            // continue to read from the socket while bRunning is set
            while (bRunning) {
                try {
                    numBytes = mInStream.read(buff);
                    Message readMsg = mHandler.obtainMessage(
                            1, numBytes, -1,
                            buff);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    mHandler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(mContext, "Input stream disconnected",   Toast.LENGTH_LONG).show();
                        }
                    });

                    // our input stream was disconnected, so we need to close our our stream
                    // and socket before trying to reconnect
                    try {
                        mInStream.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }

                    try {
                        mSocket.close();
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

        // bRunning has been set to false, close out the stream and socket
        // before completing execution
        try {
            mInStream.close();
        } catch (IOException e) {
            e.printStackTrace();

        }
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}