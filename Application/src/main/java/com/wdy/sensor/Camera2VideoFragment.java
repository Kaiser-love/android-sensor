package com.wdy.sensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.kongzue.dialog.v2.InputDialog;
import com.wdy.sensor.activity.R;
import com.wdy.sensor.utils.DialogUHelper;
import com.wdy.sensor.utils.FileUtil;
import com.wdy.sensor.utils.PoseIMURecorder;
import com.wdy.sensor.utils.SensorHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Camera2VideoFragment extends Fragment
        implements View.OnClickListener, SensorEventListener {

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "Camera2VideoFragment";
    private static final String FRAGMENT_DIALOG = "dialog";


    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private AutoFitTextureView mTextureView;

    /**
     * Button to record video
     */
    private ImageButton mButtonVideo;

    /**
     * A reference to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link android.hardware.camera2.CameraCaptureSession} for
     * preview.
     */
    private CameraCaptureSession mPreviewSession;
    // 相机属性
    private CameraCharacteristics characteristics;
    // 相机支持的FPS
    private Range<Integer>[] fpsRanges;
    // 接收视频帧回调
    private ImageReader mPreviewImageReader;

    private TextView tv;
    private TextView txt;
    // 传感器
    private SensorHelper sensorHelper;
    private PoseIMURecorder mRecorder;
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link android.util.Size} of video recording.
     */
    private Size mVideoSize;

    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    private Integer mSensorOrientation;
    private String mNextVideoAbsolutePath;
    private CaptureRequest.Builder mPreviewBuilder;

    public static Camera2VideoFragment newInstance() {
        return new Camera2VideoFragment();
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_video, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = view.findViewById(R.id.texture);
        mButtonVideo = view.findViewById(R.id.button_capture);
        mButtonVideo.setOnClickListener(this);
        view.findViewById(R.id.button_tag).setOnClickListener(this);
        view.findViewById(R.id.button_changeQuality).setOnClickListener(this);
        view.findViewById(R.id.imageButton).setOnClickListener(this);
        txt = view.findViewById(R.id.txt1);
        txt.setTextColor(-16711936);

        sensorHelper = new SensorHelper((SensorManager) Objects.requireNonNull(getActivity().getSystemService(Context.SENSOR_SERVICE)), (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE));
        tv = view.findViewById(R.id.textViewHeading);
//        view.findViewById(R.id.info).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        resume();
    }

    @Override
    public void onPause() {
        pause();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_capture: {
                if (mIsRecordingVideo) {
                    stopRecordingVideo();
                } else {
                    startRecordingVideo();
                }
                break;
            }
            case R.id.button_tag:
                addRate();
                break;
            case R.id.imageButton:
                addFrameRate();
                break;
            case R.id.button_changeQuality:
                addQuality();
                break;
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    @SuppressWarnings("MissingPermission")
    private void openCamera(int width, int height) {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];
            characteristics = manager.getCameraCharacteristics(cameraId);
            fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            options2 = new String[fpsRanges.length];
            for (int i = 0; i < fpsRanges.length; i++) {
                if (firstInit && fpsRanges[i].getLower() == 21 && fpsRanges[i].getUpper() == 21) {
                    currentOptions2Index = i;
                }
                options2[i] = fpsRanges[i].getLower() + " " + fpsRanges[i].getUpper();
            }
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
            Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
            options = new String[outputSizes.length];
            for (int i = 0; i < outputSizes.length; i++) {
                if (firstInit && outputSizes[i].getWidth() == 1280 && outputSizes[i].getHeight() == 960) {
                    currentOptionsIndex = i;
                }
                options[i] = outputSizes[i].getWidth() + " " + outputSizes[i].getHeight();
            }
            firstInit = false;
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
//            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                    width, height, mVideoSize);
            String[] size = options[currentOptionsIndex].split(" ");
            mVideoSize = new Size(Integer.valueOf(size[0]), Integer.valueOf(size[1]));
            mPreviewSize = mVideoSize;
            mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            // setUpImageReader();
            int orientation = getResources().getConfiguration().orientation;
//            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            } else {
//                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
//            }
            configureTransform(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mMediaRecorder = new MediaRecorder();
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            if (mPreviewImageReader != null) {
                mPreviewImageReader.close();
                mPreviewImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    if (mRecorder != null) {
                        mRecorder.addPhotoNumber(timestamp, currentPhotoIndex.getAndIncrement());
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        // 设置预览画面的帧率 视实际情况而定选择一个帧率范围
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges[currentOptions2Index]);
    }

    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getFilePath(0);
        }
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private String getFilePath(int type) {
        switch (type) {
            case 0:
                String absolutePath = FileUtil.setupOutputFolder(FileUtil.VIDEO_FILE_PATH);
                return absolutePath + "/" + System.currentTimeMillis() + ".mp4";
            case 1:
                absolutePath = FileUtil.setupOutputFolder(FileUtil.PHOTO_FILE_PATH);
                return absolutePath + "/" + System.currentTimeMillis() + ".csv";
            default:
                break;
        }
        return "";
    }

    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            currentPhotoIndex.set(1);
            sensorHelper.setmInitialStepCount(-1.0f);
            txt.setTextColor(-65536);
            closePreviewSession();
            setUpMediaRecorder();
            String output_dir = FileUtil.setupOutputFolder(FileUtil.SENSOR_FILE_PATH);
            mRecorder = new PoseIMURecorder(output_dir, getActivity());
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            // 设置预览画面的帧率 视实际情况而定选择一个帧率范围
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges[currentOptions2Index]);

            // 自动对焦
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            // 设置预览回调的 Surface
//            surfaces.add(mPreviewImageReader.getSurface());
//            mPreviewBuilder.addTarget(mPreviewImageReader.getSurface());


            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                    getActivity().runOnUiThread(() -> {
                        // UI
                        mIsRecordingVideo = true;
                        // Start recording
                        mMediaRecorder.start();
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private void stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false;
        txt.setTextColor(-16711936);
        currentOptions1Index = 1;
        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        if (mRecorder != null) {
            mRecorder.endFiles();
            mRecorder = null;
        }
        Activity activity = getActivity();
        if (null != activity) {
            Toast.makeText(activity, "Video saved: " + mNextVideoAbsolutePath,
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath);
        }
        mNextVideoAbsolutePath = null;
        startPreview();
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> activity.finish())
                    .create();
        }

    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_request)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    })
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, which) -> parent.getActivity().finish())
                    .create();
        }

    }

    private void resume() {
        startBackgroundThread();
        sensorHelper.registerListener(this, currentOptions1Index);
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        if (sensorHelper != null)
            sensorHelper.registerListener(this, currentOptions1Index);
    }

    private void pause() {
        closeCamera();
        stopBackgroundThread();
        sensorHelper.unregisterListener(this);
    }

    private String[] options;
    private Integer currentOptionsIndex = 10;
    private Integer currentOptions1Index = 4;
    private String[] options1 = {"SENSOR_DELAY_FASTEST", "SENSOR_DELAY_GAME", "SENSOR_DELAY_UI", "SENSOR_DELAY_NORMAL", "5"};
    private String[] options2;
    private Integer currentOptions2Index = 0;

    private Boolean firstInit = true;
    // 当前视频帧
    private AtomicInteger currentPhotoIndex = new AtomicInteger(1);

    public void addQuality() {
        DialogUHelper.shopSingleCheckableDialog(getActivity(), options, currentOptionsIndex, (dialog, which) -> {
            currentOptionsIndex = which;
            pause();
            resume();
            dialog.dismiss();
        });
    }

    public void addRate() {
        DialogUHelper.shopSingleCheckableDialog(getActivity(), options1, currentOptions1Index, (dialog, which) -> {
            if (which == options1.length - 1) {
                InputDialog.show(getActivity(), "自定义采样频率（毫秒）", "设置毫秒", (dialog1, inputText) -> {
                    options1[which] = inputText;
                    currentOptions1Index = which;
                    sensorHelper.unregisterListener(this);
                    sensorHelper.registerListener(this, Integer.valueOf(options1[which]));
                    dialog1.dismiss();
                });
            } else {
                currentOptions1Index = which;
                sensorHelper.unregisterListener(this);
                sensorHelper.registerListener(this, currentOptions1Index);
            }
            dialog.dismiss();
        });


    }

    public void addFrameRate() {
        DialogUHelper.shopSingleCheckableDialog(getActivity(), options2, currentOptions2Index, (dialog, which) -> {
            currentOptions2Index = which;
            dialog.dismiss();
        });
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // 加速度传感器
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorHelper.setAccX(event.values[0]);
            sensorHelper.setAccY(event.values[1]);
            sensorHelper.setAccZ(event.values[2]);
            sensorHelper.setAcc(event.values);
            // 获取方位角
            getValue();
            if (mIsRecordingVideo && mRecorder != null) {
                mRecorder.addIMURecord(event, PoseIMURecorder.ACCELEROMETER);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensorHelper.setGyroX(event.values[0]);
            sensorHelper.setGyroY(event.values[1]);
            sensorHelper.setGyroZ(event.values[2]);
            if (mIsRecordingVideo && mRecorder != null) {
                mRecorder.addIMURecord(event, PoseIMURecorder.GYROSCOPE);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            sensorHelper.setLinearAccX(event.values[0]);
            sensorHelper.setLinearAccY(event.values[1]);
            sensorHelper.setLinearAccZ(event.values[2]);
            if (mIsRecordingVideo && mRecorder != null) {
                mRecorder.addIMURecord(event, PoseIMURecorder.LINEAR_ACCELERATION);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            sensorHelper.setgX(event.values[0]);
            sensorHelper.setgY(event.values[1]);
            sensorHelper.setgZ(event.values[2]);
            if (mIsRecordingVideo && mRecorder != null) {
                mRecorder.addIMURecord(event, PoseIMURecorder.GRAVITY);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            sensorHelper.setGeomagnetic(event.values);
            if (mIsRecordingVideo && mRecorder != null) {
                mRecorder.addIMURecord(event, PoseIMURecorder.MAGNETOMETER);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            if (mIsRecordingVideo && mRecorder != null) {
                mRecorder.addIMURecord(event, PoseIMURecorder.ROTATION_VECTOR);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (mIsRecordingVideo) {
                if (sensorHelper.getmInitialStepCount() < 0) {
                    float mInitialStepCount = event.values[0] - 1;
                    sensorHelper.setmInitialStepCount(mInitialStepCount);
                }
                if (mIsRecordingVideo && mRecorder != null) {
                    mRecorder.addStepRecord(event.timestamp,
                            (int) (event.values[0] - sensorHelper.getmInitialStepCount()));
                }
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void getValue() {
        if (sensorHelper.getAcc() == null || sensorHelper.getGeomagnetic() == null)
            return;
        float[] r = new float[9];
        float[] values = new float[3];
        // r从这里返回
        SensorManager.getRotationMatrix(r, null, sensorHelper.getAcc(), sensorHelper.getGeomagnetic());
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
        double newHeading = azimuth;
        sensorHelper.setNewHeading(newHeading);
        sensorHelper.setLocation();
        String setTextText = "Azimuth: " + azimuth + "\nPitch:" + pitch + "\nRoll:" + roll + "\nHead: " + sensorHelper.getNewHeading() + "\nSpeed: " + sensorHelper.getSpeed();
        getActivity().runOnUiThread(() -> tv.setText(setTextText));
    }


    private void setUpImageReader() {
        mPreviewImageReader = ImageReader.newInstance(mVideoSize.getWidth(), mVideoSize.getHeight(), ImageFormat.YUV_420_888, 30);
        mPreviewImageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireNextImage();
            if (image != null) {
                long timestamp = image.getTimestamp();
                if (mRecorder != null) {
                    mRecorder.addPhotoNumber(timestamp, currentPhotoIndex.getAndIncrement());
                }
                image.close();
            }
        }, mBackgroundHandler);
    }
}
