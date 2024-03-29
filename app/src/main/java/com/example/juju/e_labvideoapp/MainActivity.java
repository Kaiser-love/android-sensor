package com.example.juju.e_labvideoapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;

import com.example.juju.e_labvideoapp.utils.DialogUHelper;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements SensorEventListener {
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mediaRecorder;
    private ImageButton capture, vid;
    private Context myContext;
    private FrameLayout cameraPreview;
    private Chronometer chrono;
    private TextView tv;
    private TextView txt;

    int quality = 0;
    int rate = 100;
    String timeStampFile;
    int clickFlag = 0;
    Timer timer;
    int samplingPeriodUs = 0;
    String videoFps = "30000 30000";
    LocationListener locationListener;
    LocationManager LM;
    // 当前视频帧
    int currentPhotoIndex = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        head = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotv = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        cameraPreview = findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);
        capture = findViewById(R.id.button_capture);
        capture.setOnClickListener(captureListener);

        chrono = findViewById(R.id.chronometer);
        txt = findViewById(R.id.txt1);
        txt.setTextColor(-16711936);

        vid = findViewById(R.id.imageButton);
//        vid.setVisibility(View.GONE);

        tv = findViewById(R.id.textViewHeading);
        String setTextText = "Heading: " + heading + " Speed: " + speed;
        tv.setText(setTextText);
        //用来保存地磁传感器的值
        geomagnetic = new float[3];
        gravity = new float[3];

    }


    public void onResume() {
        super.onResume();
        if (!checkCameraHardware(myContext)) {
            Toast toast = Toast.makeText(myContext, "Phone doesn't have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            //findBackFacingCamera()
            mCamera = Camera.open(0);
            mPreview.refreshCamera(mCamera);
            Camera.Parameters params = mCamera.getParameters();
            List<int[]> supportedPreviewFpsRange = params.getSupportedPreviewFpsRange();
            options2 = new String[supportedPreviewFpsRange.size()];
            for (int i = 0; i < supportedPreviewFpsRange.size(); i++) {
                options2[i] = supportedPreviewFpsRange.get(i)[0] + " " + supportedPreviewFpsRange.get(i)[1];
            }

        }
        // 得到设置支持的所有传感器的List
//        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
//        List<String> sensorNameList = new ArrayList<String>();
//        for (Sensor sensor : sensorList) {
//            sensorNameList.add(sensor.getName());
//        }
        sensorManager.registerListener(this, magneticSensor, currentOptions1Index);
        sensorManager.registerListener(this, accelerometer, currentOptions1Index);
        sensorManager.registerListener(this, head, currentOptions1Index);
        sensorManager.registerListener(this, gyro, currentOptions1Index);
        sensorManager.registerListener(this, rotv, currentOptions1Index);


        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.

                latitude = location.getLatitude();
                longitude = location.getLongitude();
                if (location.hasSpeed()) {
                    speed = location.getSpeed();
                }
                location.distanceBetween(latitude_original, longitude_original, latitude, longitude, dist);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Acquire a reference to the system Location Manager
        LM = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Register the listener with the Location Manager to receive location updates
        LM.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // when on Pause, release camera in order to be used from other
        // applications
        releaseCamera();
        sensorManager.unregisterListener(this);
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    boolean recording = false;
    OnClickListener captureListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (recording) {
                currentPhotoIndex = 1;
                // stop recording and release camera
                mediaRecorder.stop(); // stop the recording
                releaseMediaRecorder(); // release the MediaRecorder object
                Toast.makeText(MainActivity.this, "Video captured!", Toast.LENGTH_LONG).show();
                recording = false;
                //d.exportData();
                chrono.stop();
                chrono.setBase(SystemClock.elapsedRealtime());

                chrono.start();
                chrono.stop();
                txt.setTextColor(-16711936);
                //chrono.setBackgroundColor(0);
                enddata();
/*
                if(clickFlag == 1){
                    clickFlag = 0;
                    capture.performClick();
                }
*/
            } else {
                // 视频文件目录
                File wallpaperDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/elab/video");
                wallpaperDirectory.mkdirs();

                // 传感器时间戳文件目录
                timeStampFile = String.valueOf((new Date()).getTime());
                wallpaperDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/elab/sensor");
                wallpaperDirectory.mkdirs();

                // 视频帧时间戳文件目录
                wallpaperDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/elab/photo");
                wallpaperDirectory.mkdirs();
                String filePath = Environment.getExternalStorageDirectory().getPath() + "/elab/photo/" + timeStampFile + ".csv";
                try {
                    photoWriter = new PrintWriter(filePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                photoWriter.println("Number" + ",Timestamp");
                if (!prepareMediaRecorder()) {
                    Toast.makeText(MainActivity.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
                    finish();
                }

                // work on UiThread for better performance
                runOnUiThread(() -> {
                    try {
                        mediaRecorder.start();
                    } catch (final Exception ex) {
                    }
                });
                Toast.makeText(MainActivity.this, "Recording...", Toast.LENGTH_LONG).show();
                Camera.Parameters params = mCamera.getParameters();
                String[] s = videoFps.split(" ");
                params.setPreviewFpsRange(Integer.valueOf(s[0]), Integer.valueOf(s[1])); // 30 fps
                if (params.isAutoExposureLockSupported())
                    params.setAutoExposureLock(true);

                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.setParameters(params);
                mCamera.setPreviewCallback((bytes, camera) -> {
                    String timeStamp = String.valueOf((new Date()).getTime());
                    photoWriter.println(timeStamp + "," + currentPhotoIndex++);
                });
                //d.beginData();
                storeData();
                chrono.setBase(SystemClock.elapsedRealtime());

                chrono.start();
                //chrono.setBackgroundColor(-65536);
                txt.setTextColor(-65536);
                recording = true;

            }
        }
    };

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock(); // lock camera for later use
        }
    }

    private boolean prepareMediaRecorder() {

        try {
            mCamera.unlock();
            mediaRecorder = new MediaRecorder();

            mediaRecorder.setCamera(mCamera);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            if (quality == 0)
                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));
            else if (quality == 1)
                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
            else if (quality == 2)
                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));

            //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + "/elab/video/" + timeStampFile + ".mp4");
            // 设置录制的视频编码和音频编码
//            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
            //mediaRecorder.setMaxDuration(5000);
//            mediaRecorder.setVideoFrameRate(10);
//            mediaRecorder.setCaptureRate(rate);
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        } catch (Exception e) {
            System.out.println(e);
        }
        return true;

    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    /* --------------------- Data Section ----------------------------*/

    Location location;
    LocationManager lm;
    double latitude = 0;
    double longitude = 0;

    double latitude_original = 0;
    double longitude_original = 0;
    float speed = 0;
    float dist[] = {0, 0, 0};
    PrintWriter writer = null;
    PrintWriter photoWriter = null;
    long timechecker = 5000;


    class SayHello extends TimerTask {
        public void run() {
            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            location = getLastKnownLocation();
            lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 100, 0, locationListener);
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            latitude_original = location.getLatitude();
            longitude_original = location.getLongitude();
            if (location.hasSpeed()) {
                speed = location.getSpeed();
            }
            dist[0] = (float) 0.0;
            long elapsedMillis = SystemClock.elapsedRealtime() - chrono.getBase();
            if (elapsedMillis >= timechecker) {
                clickFlag = 1;
                timechecker = timechecker + 5000;
                timer.cancel();
                timer.purge();
            }
            String timeStamp = String.valueOf((new Date()).getTime());
            writer.println(timeStamp + "," + longitude_original + "," + latitude_original + "," + speed + "," + dist[0] + "," + timeStamp + "," + linear_acc_x + "," + linear_acc_y + "," + linear_acc_z + "," +
                    heading + "," + newHeading + "," + gyro_x + "," + gyro_y + "," + gyro_z);
//            String timeStamp = String.valueOf((new Date()).getTime());
//            writer.println(timeStamp + "," +
//                    longitude_original + "," + latitude_original + "," +
//                    rotv_x + "," + rotv_y + "," + rotv_z + "," + rotv_w + "," + rotv_accuracy);
        }

        private Location getLastKnownLocation() {
            LocationManager mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            List<String> providers = mLocationManager.getProviders(true);
            Location bestLocation = null;
            for (String provider : providers) {
                Location l = mLocationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            }
            return bestLocation;
        }
    }

    public void storeData() {

        String filePath = Environment.getExternalStorageDirectory().getPath() + "/elab/sensor/" + timeStampFile + ".csv";
        try {
            writer = new PrintWriter(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        writer.println("Timestamp" + "," + "Longitude" + "," + "Latitude" + "," + "Speed" + "," + "Distance" + "," + "Time" + "," + "Acc X" + "," + "Acc Y" + "," + "Acc Z" + "," + "Heading" + "," + "NewHeading"
                + "," + "gyro_x" + "," + "gyro_y" + "," + "gyro_z");
        LocationManager original = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location original_location = original.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (original.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) != null) {
            latitude_original = original_location.getLatitude();
            longitude_original = original_location.getLongitude();
        }
        //String setTextText = "Heading: " + heading + " Speed: " + speed;
        //tv.setText(setTextText);
        timer = new Timer();
        timer.schedule(new SayHello(), 0, rate);
        /*if(clickFlag == 1) {
            capture.performClick();
        }
        */
    }

    public void enddata() {
        writer.close();
        photoWriter.close();
    }


    /* ---------------------- Sensor data ------------------- */

    private SensorManager sensorManager;
    // 地磁传感器
    private Sensor magneticSensor;
    private Sensor accelerometer;
    private Sensor head;
    private Sensor gyro;
    private Sensor rotv;

    float linear_acc_x = 0;
    float linear_acc_y = 0;
    float linear_acc_z = 0;
    float[] geomagnetic, gravity;
    float heading = 0;
    double newHeading = 0;
    float gyro_x = 0;
    float gyro_y = 0;
    float gyro_z = 0;

    float rotv_x = 0;
    float rotv_y = 0;
    float rotv_z = 0;
    float rotv_w = 0;
    float rotv_accuracy = 0;

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            rotv_x = event.values[0];
            rotv_y = event.values[1];
            rotv_z = event.values[2];
            rotv_w = event.values[3];
            rotv_accuracy = event.values[4];
        }
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            heading = Math.round(event.values[0]);
            if (heading >= 270) {
                heading = heading + 90;
                heading = heading - 360;
            } else {
                heading = heading + 90;
            }
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
            // 获取方位角
            getValue();
            linear_acc_x = event.values[0];
            linear_acc_y = event.values[1];
            linear_acc_z = event.values[2];

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyro_x = event.values[0];
            gyro_y = event.values[1];
            gyro_z = event.values[2];
        }
    }

    String[] options = {"1080p", "720p", "480p"};
    Integer currentOptions1Index = 0;
    String[] options1 = {"SENSOR_DELAY_FASTEST", "SENSOR_DELAY_GAME", "SENSOR_DELAY_UI", "SENSOR_DELAY_NORMAL"};
    String[] options2;
    Integer currentOptions2Index = 0;

    public void getValue() {
        float[] r = new float[9];
        float[] values = new float[3];
        // r从这里返回
        SensorManager.getRotationMatrix(r, null, gravity, geomagnetic);
        //values从这里返回
        SensorManager.getOrientation(r, values);
        //提取数据
        double azimuth = Math.toDegrees(values[0]);
        if (azimuth < 0) {
            azimuth = azimuth + 360;
        }
        double pitch = Math.toDegrees(values[1]);
        double roll = Math.toDegrees(values[2]);
        // 方位
        newHeading = azimuth;
        String setTextText = "Azimuth: " + azimuth + "\nPitch:" + pitch + "\nRoll:" + roll + "\nHead: " + heading + "\nSpeed: " + speed;
        tv.setText(setTextText);
    }


    public void addQuality(View view) {
        DialogUHelper.shopSingleCheckableDialog(this, options, quality, (dialog, which) -> {
            quality = which;
            dialog.dismiss();
        });
    }

    public void addRate(View view) {
        DialogUHelper.shopSingleCheckableDialog(this, options1, currentOptions1Index, (dialog, which) -> {
            samplingPeriodUs = which;
            currentOptions1Index = which;
            sensorManager.unregisterListener(this);
            sensorManager.registerListener(this, magneticSensor, currentOptions1Index);
            sensorManager.registerListener(this, accelerometer, currentOptions1Index);
            sensorManager.registerListener(this, head, currentOptions1Index);
            sensorManager.registerListener(this, gyro, currentOptions1Index);
            sensorManager.registerListener(this, rotv, currentOptions1Index);
            dialog.dismiss();
        });


    }

    public void addFrameRate(View view) {
        DialogUHelper.shopSingleCheckableDialog(this, options2, currentOptions2Index, (dialog, which) -> {
            videoFps = options2[which];
            currentOptions2Index = which;
            dialog.dismiss();
        });
    }

}