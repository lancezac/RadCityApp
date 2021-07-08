package com.example.radcityapp;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Fall detection class for detecting a user fall
 */
public class FallDetection implements SensorEventListener {
    private boolean m_bEnabledState = false;
    private String mPhoneNum = "";
    private Location mCurrentLoc;
    private SensorManager mSensorMgr;
    private Handler mHandler = null;
    private Context mContext = null;

    /**
     * Constructor
     * @param activity
     * @param handler
     * @param context
     */
    public FallDetection(Activity activity, Handler handler, Context context){
        mSensorMgr = (SensorManager) activity.getSystemService(SENSOR_SERVICE);
        mSensorMgr.registerListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mHandler = handler;
        mContext = context;
    }

    /**
     * set the current location
     * @param mCurrentLoc
     */
    public void setCurrentLoc(Location mCurrentLoc) {
        this.mCurrentLoc = mCurrentLoc;
    }

    /**
     * get enabled state
     * @return
     */
    boolean getEnabledState(){return m_bEnabledState;}

    /**
     * set the enabled state
     * @param b
     */
    void setEnabledState(boolean b){m_bEnabledState = b;}

    /**
     * get user set phone number
     * @return
     */
    String getPhoneNum(){return mPhoneNum;}

    /**
     * Set the phone number on user command
     * @param s
     */
    void setPhoneNum(String s){mPhoneNum = s;}

    /**
     * Send SMS message in the event of an accident
     */
    void sendSMS(){
        SmsManager sMM = SmsManager.getDefault();
        String message = String.format("(THIS IS A TEST) $1 $2", mCurrentLoc.getLatitude(), mCurrentLoc.getLongitude());
        sMM.sendTextMessage(mPhoneNum, null, message, null, null);
    }

    /**
     * determine if the user has crashed
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (m_bEnabledState) {
            if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];

                double t = Math.sqrt(Math.pow(Math.abs(x),2) + Math.pow(Math.abs(y),2) + Math.pow(Math.abs(z),2));
                Log.d("ACCEL", Double.toString(t));

                // constant of 150 picked through experimentation
                if (t >= 150) {
                    //sendSMS();
                    byte[] buff = new byte[1024];
                    int numBytes;
                    mHandler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(mContext, "Accident detected, sending text message...", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }

    /**
     * make changes based on accuracy update, not needed for this application
     * @param sensor
     * @param accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
