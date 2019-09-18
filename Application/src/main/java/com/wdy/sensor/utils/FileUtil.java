package com.wdy.sensor.utils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FileUtil {

    public static final String SAVE_DIR = "/sdcard/DCIM/Camera2GetPreview/";

    public static boolean saveBytes(byte[] bytes, String imagePath) {
        File file = new File(imagePath);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveBitmap(Bitmap bitmap, String imagePath) {
        if (bitmap == null) {
            return false;
        }
        File file = new File(imagePath);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String SENSOR_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/AR/sensor";
    public static String VIDEO_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/AR/vedio";
    public static String PHOTO_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/AR/photo";

    public static String mkdirs(String path) {
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                System.out.println("创建目录失败");
            }
        }
        return file.getAbsolutePath();
    }

    public static String setupOutputFolder(String path) {
        Calendar current_time = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss", Locale.US);
        File output_dir = new File(path,
                formatter.format(current_time.getTime()));
        if (!output_dir.exists()) {
            if (!output_dir.mkdirs()) {
                System.out.println("创建目录失败");
            }
        }
        return output_dir.getAbsolutePath();
    }
}