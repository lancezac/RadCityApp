package com.example.radcityapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;



public class MainActivity extends AppCompatActivity implements LocationListener,
        Application.ActivityLifecycleCallbacks, FallDetectionDlg.FallDetectionDlgListener {
    String CHANNEL_ID="1";
    private double mAmbientTemp = 0.0;
    private double mAmbientHumidity = 0.0;
    private double mAmbientPressure = 0.0;
    private double mAmbientAlt = 0.0;
    private double mAmps = 0.0;
    private double mBrakeTempC = 0.0;
    private double mBatteryTempC = 0.0;
    private double mBatteryVoltage = 0.0;
    private int mPASLevel = 0;
    private int mHeadlightState = 0;
    private float mSpeed = 0.f;
    private boolean m_bFirstReading = true;
    private double mInitialBatteryReading = 0;
    private boolean m_bConnect = false;
    private boolean mbOkToSend10MileWarning = true;
    private boolean mbOkToSend5MileWarning = true;


    private double mDistanceTraveled = 0;
    private Location mPrevLoc;
    private boolean mFirstDistanceCalc = true;

    private LocationManager mLocationManager;
    private FallDetection mFallDetection;
    PiConnectThread mConnection = null;

    TextView mBatteryChargeView;
    TextView mRangeEstimateView;
    TextView mBatteryVoltageView;
    TextView mPASView;
    TextView mPowerUsageView;
    TextView mAmbientTempView;
    TextView mAmbientHumidityView;
    TextView mBatteryTempView;
    TextView mFrontBrakeTempView;
    TextView mPressureView;
    TextView mAltitudeView;
    TextView mBikePowerView;
    TextView mHeadlightPowerView;
    TextView mGPSSpeedView;
    TextView mWattHoursPerMileView;

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        /**
         * handle the message received over bluetooth
         */
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    byte[] readBuf = (byte[]) msg.obj;
                    String read = new String(readBuf, 0, msg.arg1);
                    List<String> piData = Arrays.asList(read.split(","));
                    // complete message, ok to parse
                    if (piData.size() == 10) {
                        mAmbientTemp = Double.parseDouble(piData.get(0));
                        mAmbientHumidity = Double.parseDouble(piData.get(1));
                        mAmbientPressure = Double.parseDouble(piData.get(2));
                        mAmbientAlt = Double.parseDouble(piData.get(3));
                        mAmps = Double.parseDouble(piData.get(4));
                        mBrakeTempC = Double.parseDouble(piData.get(5));
                        mBatteryTempC = Double.parseDouble(piData.get(6));
                        mBatteryVoltage = Double.parseDouble(piData.get(7));
                        mPASLevel = (int) Double.parseDouble(piData.get(8));
                        mHeadlightState = ((int) Double.parseDouble(piData.get(9)));
                        updateGUI();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getApplication().registerActivityLifecycleCallbacks(this);

        // create location manager instance
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // make sure we have all required permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, 1);
        }


        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, this);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }


        // initialize member variables


        mRangeEstimateView = (TextView) findViewById(R.id.RangeEstimate);
        mBatteryChargeView = (TextView) findViewById(R.id.BatteryChargePercent);
        mBatteryVoltageView = (TextView) findViewById(R.id.BatteryVoltage);
        mPASView = (TextView) findViewById(R.id.PASLevel);
        mPowerUsageView = (TextView) findViewById(R.id.PowerUsage);
        mAmbientTempView = (TextView) findViewById(R.id.AmbientTemperature);
        mAmbientHumidityView = (TextView) findViewById(R.id.AmbientHumidity);

        mPressureView = (TextView) findViewById(R.id.Pressure);
        mAltitudeView = (TextView) findViewById(R.id.Altitude);
        mBikePowerView = (TextView) findViewById(R.id.BikePowerState);
        mHeadlightPowerView = (TextView) findViewById(R.id.HeadlightState);
        mGPSSpeedView = (TextView) findViewById(R.id.GPSSpeed);
        mWattHoursPerMileView = (TextView) findViewById(R.id.WattHoursPerMile);

        mFallDetection = new FallDetection(this, mHandler, getApplicationContext());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "RadCityAppNotifications";
            String description = "Notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }

    /**
     * update the GUI with information from the Pi
     */
    private void updateGUI(){

        double chargeEstimate = FortyEightVoltBatteryLevelEstimator.getBatteryLevelEstimate(mBatteryVoltage);
        double rangeEstimate = RangeEstimator.EstimateRange(mPASLevel, chargeEstimate);

        if (rangeEstimate > 0){
            if (rangeEstimate < 20 && mbOkToSend10MileWarning){
                publishNotification(rangeEstimate);
                mbOkToSend10MileWarning = false;
            }

            if (rangeEstimate < 5 && mbOkToSend5MileWarning){
                publishNotification(rangeEstimate);
                mbOkToSend5MileWarning = false;
            }

            if (rangeEstimate >= 20){
                mbOkToSend10MileWarning = true;
            }

            if (rangeEstimate >=5 ){
                mbOkToSend5MileWarning = true;
            }
        }

        mAmbientTempView.setText(String.format(Locale.ENGLISH,"%1$,.0f F", CtoF(mAmbientTemp)));
        mAmbientHumidityView.setText(String.format(Locale.ENGLISH,"%1$,.0f %%", mAmbientHumidity));
        mPressureView.setText(String.format(Locale.ENGLISH,"%1$,.0f hPa", mAmbientPressure));
        // convert alt from m to ft
        mAltitudeView.setText(String.format(Locale.ENGLISH,"%1$,.0f ft", mAmbientAlt * 3.28084));


        String powerState = "Off";
        // only display certain fields when the bike is on
        if (mBatteryVoltage > 0)
        {
            mBatteryChargeView.setText(String.format(Locale.ENGLISH,"%1$,.0f %%", chargeEstimate));
            mRangeEstimateView.setText(String.format(Locale.ENGLISH,"%1$,.0f mi", rangeEstimate));
            mBatteryVoltageView.setText(String.format(Locale.ENGLISH,"%1$,.0f V", mBatteryVoltage));
            mPASView.setText(Integer.toString(mPASLevel));

            // read amps divided by 48 (max value for the parsed field) multiplied by 16, the max
            // amps that can be provided by the controller
            Log.d("AMPS", Double.toString(mAmps));
            double amps = mAmps/48 * 16;

            if (m_bFirstReading && chargeEstimate > 0){
                mInitialBatteryReading = RangeEstimator.getRemainingCapacity(chargeEstimate);
                m_bFirstReading = false;
            }

            // W = A * V
            double powerDraw = amps * mBatteryVoltage;

            double wattHoursPerMile = 0;
            double remainingEstimate = RangeEstimator.getRemainingCapacity(chargeEstimate);
            if(mDistanceTraveled > 0 && remainingEstimate < mInitialBatteryReading) {
                wattHoursPerMile = (mInitialBatteryReading - remainingEstimate)/(0.000621371 * mDistanceTraveled);
            }

            mWattHoursPerMileView.setText(String.format(Locale.ENGLISH,"%1$,.0f", wattHoursPerMile));
            mPowerUsageView.setText(String.format(Locale.ENGLISH,"%1$,.0f W", powerDraw));

            powerState = "On";
        }
        else {
            mBatteryChargeView.setText("N/A");
            mRangeEstimateView.setText("N/A");
            mBatteryVoltageView.setText("N/A");
            mPASView.setText("N/A");
            mPowerUsageView.setText("N/A");
            mWattHoursPerMileView.setText("N/A");
        }

        mBikePowerView.setText(powerState);

        String headlightState = "Off";
        if(mHeadlightState > 0) {
            headlightState = "On";
        }

        mHeadlightPowerView.setText(headlightState);
    }

    /**
     * helper function to convert to C to F
     * @param temp
     * @return
     */
    private double CtoF(double temp){
        return ((9.0/5.0) * temp + 32);
    }

    @Override
    /**
     *  respond to location update
     */
    public void onLocationChanged(Location location){
        mSpeed = location.getSpeed();
        mFallDetection.setCurrentLoc(location);

        // convert speed from m/s to mph
        mGPSSpeedView.setText(String.format(Locale.ENGLISH, "%1$,.1f mph", mSpeed * 2.2369));
        if (mFirstDistanceCalc){
            mPrevLoc = location;
            mFirstDistanceCalc = false;
            return;
        }

        mDistanceTraveled += mPrevLoc.distanceTo(location);
        mPrevLoc = location;
    }

    public void onSelectFallDlg(View view){
        FallDetectionDlg dlg = new FallDetectionDlg();
        Bundle args = new Bundle();
        args.putBoolean("enabled", mFallDetection.getEnabledState());
        args.putString("phoneNum", mFallDetection.getPhoneNum());
        dlg.setArguments(args);
        dlg.show(getSupportFragmentManager(), "config fall");
    }

    public void onConnect(View view){
        // start the Pi connect thread to handle receiving data from the Pi
        Button b = (Button) findViewById(R.id.Connect);
        if (!m_bConnect) {
            mConnection = new PiConnectThread(mHandler, getApplicationContext());
            mConnection.start();
            m_bConnect = true;
            b.setText("Disconnect");
        }
        else {
            mConnection.bRunning = false;
            m_bConnect = false;
            b.setText("Connect");
        }
    }

    public void publishNotification(double remainingRange){
        String message = "";

        message = Integer.toString((int)remainingRange) + " miles remaining, decrease PAS level";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Range Notification")
                .setSmallIcon(R.drawable.radcity)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notificationManager.notify(1, builder.build());
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        mConnection.bRunning = false;
    }

    @Override
    public void onDialogPositiveClick(FallDetectionDlg dlg) {
        mFallDetection.setEnabledState(dlg.mSwitch.isChecked());
        mFallDetection.setPhoneNum(dlg.mTextView.getText().toString());
    }

}