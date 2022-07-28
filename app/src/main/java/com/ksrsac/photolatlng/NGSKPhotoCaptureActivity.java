package com.ksrsac.photolatlng;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NGSKPhotoCaptureActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, GPSTracker.locationChanged, CameraControllerV2WithPreview.PhotoTakenCompleted, SensorEventListener {
    private static final String TAG = "PhotoCaptureActivity";
    CameraControllerV2WithPreview ccv2WithPreview;
    CameraControllerV2WithoutPreview ccv2WithoutPreview;

    AutoFitTextureView textureView;
    Switch startstoppreview;
    LinearLayout frameLayoutView;
    private static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;
    String latitude, longitude;
    private TextView latText, timeText, mTextViewWorkName, proposal_id ;
    private String line1, line2, line3;
    GPSTracker gpsTracker;
    int height, width;
    private File mFile;
    private SensorManager mSensorManager;
    private Sensor mOrientation;

    // private TextView textViewPitch, textViewRoll, textViewAzimuth;
    private ProgressBar mProgressBar;
    DecimalFormat df = new DecimalFormat();
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ngsk_activity_photocapture);

        mProgressBar = findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);
        latText = findViewById(R.id.latitude);
        timeText = findViewById(R.id.time);
        proposal_id = findViewById(R.id.tv_id);
        mTextViewWorkName = findViewById(R.id.work_name);
        df.setMaximumFractionDigits(7);

        String str_proposal_id = getIntent().getStringExtra("PROP_ID");
        String workName = getIntent().getStringExtra("WorkName");
        if(workName==null || workName.isEmpty())
            workName = "Work Name";

        if(str_proposal_id==null|| str_proposal_id.isEmpty())
            str_proposal_id = "Prop_ID";
        proposal_id.setText(getString(R.string.work_id)+":"+str_proposal_id);
        mTextViewWorkName.setText(getString(R.string.work_name)+":"+workName);
     //   line3 = mTextViewWorkName.getText().toString();
        line3 = mTextViewWorkName.getText().toString()+" my text\nNext line is very long text that does not definitely fit in a single line line is very long text that does not definitely fit in a single line  on an android device. This will show you how!";

        get_location_data();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList  =
                mSensorManager.getSensorList(Sensor.TYPE_ALL);
        if(sensorList.contains( mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION))){
            mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        }else{
            //  Toast.makeText(DataCollection_Tanks.this,"No sensor detected, photo angel cannot be captured.",Toast.LENGTH_SHORT).show();
        }
        mFile = new File(getIntent().getStringExtra("FILE"));

        gpsTracker = new GPSTracker(getApplicationContext(), this);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
         height = displayMetrics.heightPixels;
       //  width = displayMetrics.widthPixels;
        width = 2048;


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            OnGPS();
        } else {
            getLocation();
        }
       // boolean showpreview = intent.getBooleanExtra("showpreview", false);
        boolean showpreview = true;

        textureView = (AutoFitTextureView)findViewById(R.id.textureview);
        frameLayoutView = findViewById(R.id.framelayout);

        startstoppreview = (Switch) findViewById(R.id.startstoppreview);
        if(showpreview) {
            ccv2WithPreview = new CameraControllerV2WithPreview(NGSKPhotoCaptureActivity.this, textureView);
            startstoppreview.setChecked(true);
        } else {
            ccv2WithoutPreview = new CameraControllerV2WithoutPreview(getApplicationContext());
            startstoppreview.setChecked(false);
        }
    /*    startstoppreview.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(startstoppreview.isChecked()) {
                    intent.putExtra("showpreview", true);
                    finish();
                    startActivity(intent);

                } else {
                    intent.putExtra("showpreview", false);
                    finish();
                    startActivity(intent);
                }
            }
        });
*/
        findViewById(R.id.getpicture).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(startstoppreview.isChecked() && ccv2WithPreview != null) {
                   // mFile =  getOutputMediaFile();
                   boolean isLandscape = false;
                  //  if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

                        if (getScreenOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
                        isLandscape = true;
                    } else {
                       isLandscape = false;
                    }
                    Log.d(TAG, "isLandscape: "+isLandscape);
                    ccv2WithPreview.takePicture(getBitmapFromView(frameLayoutView),mFile, line1, line2, line3, isLandscape);
                } else if(ccv2WithoutPreview != null){
                    ccv2WithoutPreview.openCamera();
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ccv2WithoutPreview.takePicture();
                }
                mProgressBar.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Picture Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        getPermissions();
    }

    public int getScreenOrientation()
    {
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        Log.d(TAG, "getScreenOrientation: "+ Surface.ROTATION_0);
        Display getOrient = getWindowManager().getDefaultDisplay();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        Log.d(TAG, "getScreenOrientation: "+degrees);

        int orientation = Configuration.ORIENTATION_UNDEFINED;
        if(getOrient.getWidth()==getOrient.getHeight()){
            orientation = Configuration.ORIENTATION_SQUARE;
        } else{
            if(getOrient.getWidth() < getOrient.getHeight()){
                orientation = Configuration.ORIENTATION_PORTRAIT;
            }else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }


    private File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera2TestNew");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", new  DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                NGSKPhotoCaptureActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                NGSKPhotoCaptureActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null) {
                double lat = locationGPS.getLatitude();
                double longi = locationGPS.getLongitude();
                latitude = String.valueOf(lat);
                longitude = String.valueOf(longi);
                setLatLong(locationGPS);
            } else {
                Toast.makeText(this, "Unable to find location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setLatLong(Location locationGPS) {
        latText.setText("Lat:"+df.format(locationGPS.getLatitude())+", Lon:"+df.format(locationGPS.getLongitude()));
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        timeText.setText("Date & Time:"+ sdf.format(date) );
        line1 = proposal_id.getText().toString()+", Lat:"+df.format(locationGPS.getLatitude())+" Long:"+df.format(locationGPS.getLongitude());
        line2 = "Date & Time:"+ sdf.format(date);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        float azimuth_angle = event.values[0];
        float pitch_angle = event.values[1];
        float roll_angle = event.values[2];
       // textViewAzimuth.setText("Azimuth\n"+String.valueOf(azimuth_angle));
       // textViewPitch.setText("pitch\n"+String.valueOf(pitch_angle));
      //  textViewRoll.setText("Roll\n"+String.valueOf(roll_angle));

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged: "+accuracy);
    }

    public static int convertDpToPixels(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, Resources.getSystem().getDisplayMetrics()));
    }

    public @NonNull
    Bitmap returnBitmap(){
        return BitmapFactory.decodeResource(getResources(), R.drawable.logo);
    }
    static Bitmap createBitmapFromView(@NonNull View view, int width, int height) {
        if (width > 0 && height > 0) {
            view.measure(View.MeasureSpec.makeMeasureSpec(convertDpToPixels(width), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(convertDpToPixels(height), View.MeasureSpec.EXACTLY));
        }

        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);

        return bitmap;
    }
    public Bitmap getBitmapFromView(View view) {
        view.setDrawingCacheEnabled(true);
        // this is the important code :)
        // Without it the view will have a dimension of 0,0 and the bitmap will be null

        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        view.layout(0, 0, width, view.getMeasuredHeight());

        view.buildDrawingCache(false);
        Bitmap b = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(ccv2WithPreview != null) {
//            ccv2WithPreview.closeCamera();
//        }
//        if(ccv2WithoutPreview != null) {
//            ccv2WithoutPreview.closeCamera();
//        }
    }

    private void getPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //Requesting permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override //Override from ActivityCompat.OnRequestPermissionsResultCallback Interface
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                }
                return;
            }
        }
    }

    @Override
    public void locationchange(Location location) {
        setLatLong(location);
    }

    @Override
    public void completed() {
        Log.d(TAG, "completed photo taken");
        sendData();
    }
    private void sendData()
    {
        Intent data = new Intent();
        data.putExtra("FILE", mFile.toString());
        setResult(RESULT_OK, data);
        finish();
    }

    LocationListener mlocListener;
    LocationManager mlocManager;
    static Location location_loc;

    public void get_location_data() {
        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mlocListener = new MyLocationListener();
        if (!mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !mlocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && !mlocManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            showGPSDisabledAlertToUser();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
        } else {
            if (mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location_loc = mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else if (mlocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location_loc = mlocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } else if (mlocManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                location_loc = mlocManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
            if (mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mlocListener);
            } else if (mlocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, mlocListener);
            } else if (mlocManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                mlocManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000, 0, mlocListener);
            }
        }
    }

    public class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            location_loc = loc;
            setLatLong(loc);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), getString(R.string.gps_disable1), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Toast.makeText(getApplicationContext(), "GPS enabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    private void showGPSDisabledAlertToUser() {
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(getString(R.string.gps_disable))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.settings), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  InternetOn="internetStatus";

                        Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);

                    }
                });

        alertDialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        android.app.AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        AppUtils.Constants.MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        AppUtils.Constants.MY_PERMISSIONS_REQUEST_LOCATION);
            }

        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
          //  Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
           // textureView.setRotation(90);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
           // Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
           // textureView.setRotation(0);
        }
    }
}