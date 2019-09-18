package com.wdy.sensor.utils;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.List;


public class SensorHelper {
    private SensorManager sensorManager;
    private LocationListener locationListener;
    private LocationManager LM;
    private double latitude = 0;
    private double longitude = 0;
    private double latitude_original = 0;
    private double longitude_original = 0;
    private float dist[] = {0, 0, 0};
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mGravity;
    private Sensor mLinearAcce;
    private Sensor mOrientation;
    private Sensor mMagnetometer;
    private Sensor mStepCounter;
    // 地磁传感器
    private Sensor magneticSensor;
    private float accX;
    private float accY;
    private float accZ;

    private float linearAccX;
    private float linearAccY;
    private float linearAccZ;
    private float[] geomagnetic, acc;

    private float heading;
    private double newHeading;

    private float gyroX;
    private float gyroY;
    private float gyroZ;

    private float gX;
    private float gY;
    private float gZ;

    private float rotvX;
    private float rotvY;
    private float rotvZ;
    private float rotvW;
    private float rotvAccuracy;


    private float speed;
    private float mInitialStepCount = -1;

    @SuppressLint("MissingPermission")
    public SensorHelper(SensorManager sensorManager, LocationManager LM) {
        // 获取传感器
        this.sensorManager = sensorManager;

        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mLinearAcce = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mOrientation = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        mMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mStepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

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
        this.LM = LM;
        // Register the listener with the Location Manager to receive location updates
        this.LM.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);
    }

    public void registerListener(SensorEventListener callback, int currentOptions1Index) {
        sensorManager.registerListener(callback, magneticSensor, currentOptions1Index);
        sensorManager.registerListener(callback, mAccelerometer, currentOptions1Index);
        sensorManager.registerListener(callback, mGyroscope, currentOptions1Index);
        sensorManager.registerListener(callback, mGravity, currentOptions1Index);
        sensorManager.registerListener(callback, mLinearAcce, currentOptions1Index);
        sensorManager.registerListener(callback, mOrientation, currentOptions1Index);
        sensorManager.registerListener(callback, mMagnetometer, currentOptions1Index);
        sensorManager.registerListener(callback, mStepCounter, currentOptions1Index);
    }
    public void unregisterListener(SensorEventListener callback) {
        sensorManager.unregisterListener(callback, magneticSensor);
        sensorManager.unregisterListener(callback, mAccelerometer);
        sensorManager.unregisterListener(callback, mGyroscope);
        sensorManager.unregisterListener(callback, mGravity);
        sensorManager.unregisterListener(callback, mLinearAcce);
        sensorManager.unregisterListener(callback, mOrientation);
        sensorManager.unregisterListener(callback, mMagnetometer);
        sensorManager.unregisterListener(callback, mStepCounter);

    }

    public SensorManager getSensorManager() {
        return sensorManager;
    }

    public void setSensorManager(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }

    public float getmInitialStepCount() {
        return mInitialStepCount;
    }

    public void setmInitialStepCount(float mInitialStepCount) {
        this.mInitialStepCount = mInitialStepCount;
    }

    public Sensor getmAccelerometer() {
        return mAccelerometer;
    }

    public void setmAccelerometer(Sensor mAccelerometer) {
        this.mAccelerometer = mAccelerometer;
    }

    public Sensor getmGyroscope() {
        return mGyroscope;
    }

    public void setmGyroscope(Sensor mGyroscope) {
        this.mGyroscope = mGyroscope;
    }

    public Sensor getmGravity() {
        return mGravity;
    }

    public void setmGravity(Sensor mGravity) {
        this.mGravity = mGravity;
    }

    public Sensor getmLinearAcce() {
        return mLinearAcce;
    }

    public void setmLinearAcce(Sensor mLinearAcce) {
        this.mLinearAcce = mLinearAcce;
    }

    public Sensor getmOrientation() {
        return mOrientation;
    }

    public void setmOrientation(Sensor mOrientation) {
        this.mOrientation = mOrientation;
    }

    public Sensor getmMagnetometer() {
        return mMagnetometer;
    }

    public void setmMagnetometer(Sensor mMagnetometer) {
        this.mMagnetometer = mMagnetometer;
    }

    public Sensor getmStepCounter() {
        return mStepCounter;
    }

    public void setmStepCounter(Sensor mStepCounter) {
        this.mStepCounter = mStepCounter;
    }

    public float getAccX() {
        return accX;
    }

    public void setAccX(float accX) {
        this.accX = accX;
    }

    public float getAccY() {
        return accY;
    }

    public void setAccY(float accY) {
        this.accY = accY;
    }

    public float getAccZ() {
        return accZ;
    }

    public void setAccZ(float accZ) {
        this.accZ = accZ;
    }

    public float getLinearAccX() {
        return linearAccX;
    }

    public void setLinearAccX(float linearAccX) {
        this.linearAccX = linearAccX;
    }

    public float getLinearAccY() {
        return linearAccY;
    }

    public void setLinearAccY(float linearAccY) {
        this.linearAccY = linearAccY;
    }

    public float getLinearAccZ() {
        return linearAccZ;
    }

    public void setLinearAccZ(float linearAccZ) {
        this.linearAccZ = linearAccZ;
    }

    public float[] getGeomagnetic() {
        return geomagnetic;
    }

    public void setGeomagnetic(float[] geomagnetic) {
        this.geomagnetic = geomagnetic;
    }

    public float[] getAcc() {
        return acc;
    }

    public void setAcc(float[] acc) {
        this.acc = acc;
    }

    public float getHeading() {
        return heading;
    }

    public void setHeading(float heading) {
        this.heading = heading;
    }

    public double getNewHeading() {
        return newHeading;
    }

    public void setNewHeading(double newHeading) {
        this.newHeading = newHeading;
    }

    public float getGyroX() {
        return gyroX;
    }

    public void setGyroX(float gyroX) {
        this.gyroX = gyroX;
    }

    public float getGyroY() {
        return gyroY;
    }

    public void setGyroY(float gyroY) {
        this.gyroY = gyroY;
    }

    public float getGyroZ() {
        return gyroZ;
    }

    public void setGyroZ(float gyroZ) {
        this.gyroZ = gyroZ;
    }

    public float getgX() {
        return gX;
    }

    public void setgX(float gX) {
        this.gX = gX;
    }

    public float getgY() {
        return gY;
    }

    public void setgY(float gY) {
        this.gY = gY;
    }

    public float getgZ() {
        return gZ;
    }

    public void setgZ(float gZ) {
        this.gZ = gZ;
    }

    public float getRotvX() {
        return rotvX;
    }

    public void setRotvX(float rotvX) {
        this.rotvX = rotvX;
    }

    public float getRotvY() {
        return rotvY;
    }

    public void setRotvY(float rotvY) {
        this.rotvY = rotvY;
    }

    public float getRotvZ() {
        return rotvZ;
    }

    public void setRotvZ(float rotvZ) {
        this.rotvZ = rotvZ;
    }

    public float getRotvW() {
        return rotvW;
    }

    public void setRotvW(float rotvW) {
        this.rotvW = rotvW;
    }

    public float getRotvAccuracy() {
        return rotvAccuracy;
    }

    public void setRotvAccuracy(float rotvAccuracy) {
        this.rotvAccuracy = rotvAccuracy;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setLocation() {
        List<String> providers = LM.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            @SuppressLint("MissingPermission") Location l = LM.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }

        longitude = bestLocation.getLongitude();
        latitude = bestLocation.getLatitude();
        latitude_original = bestLocation.getLatitude();
        longitude_original = bestLocation.getLongitude();
        if (bestLocation.hasSpeed()) {
            speed = bestLocation.getSpeed();
        }
        longitude = bestLocation.getLongitude();
        latitude = bestLocation.getLatitude();
        latitude_original = bestLocation.getLatitude();
        longitude_original = bestLocation.getLongitude();
        if (bestLocation.hasSpeed()) {
            speed = bestLocation.getSpeed();
        }
    }
}
