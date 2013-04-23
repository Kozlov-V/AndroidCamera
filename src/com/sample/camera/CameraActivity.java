package com.sample.camera;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.example.camera.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


import com.example.camera.R;

public class CameraActivity extends Activity {

    public static final int 	MEDIA_TYPE_IMAGE = 1;
    public static final int		MEDIA_TYPE_VIDEO = 2;

    private static final String TAG = "camera->CameraActivity";

    private static boolean		isRecording = false;

    private Camera myCamera = null;

    private MyCameraSurfaceView myCameraSurfaceView;
    private FrameLayout myCameraPreviewFrameLayout;

    private Camera.ErrorCallback myCameraErrorCallback;
    private Camera.PictureCallback myPictureCallback;

    private MediaRecorder myMediaRecorder;
    private MediaRecorder.OnInfoListener myMediaRecorderOnInfoListener;

    private Button btnRecordVideo, btnGoRec;

    @Override
    public void onCreate (Bundle savedInstanceState) {

        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
       // setContentView (R.layout.activity_pre);

        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //btnTakePhoto	= (Button) findViewById (R.id.btnTakePhoto);
        btnRecordVideo	= (Button) findViewById (R.id.btnRecordVideo);
        //btnGoRec	    = (Button) findViewById (R.id.btnGoRec);

        //btnTakePhoto.setOnClickListener		(takePhotoButtonOnClickListener);
        btnRecordVideo.setOnClickListener	(recordVideoButtonOnClickListener);
        btnGoRec.setOnClickListener	        (goRecButtonOnClickListener);

        myPictureCallback = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken (byte[] data, Camera camera) {				//TODO: Save GPS details to JPEG EXIF here?

                Log.i(TAG, "Entering myPictureCallback->onPictureTaken");

                File pictureFile = getOutputMediaFile (MEDIA_TYPE_IMAGE);

                if (pictureFile == null) {

                    Log.e (TAG, "->myPictureCallback->onPictureTaken Error creating image file... check storage permissions");

                    return;
                }

                try {

                    FileOutputStream fos = new FileOutputStream (pictureFile);

                    fos.write (data);
                    fos.close();

                    Log.i (TAG, "->myPictureCallback->onPictureTaken Image Saved");
                }

                catch (FileNotFoundException e) { Log.e (TAG, "->myPictureCallback->onPictureTaken Image file not found: " + e.getMessage()); }

                catch (IOException e) { Log.e (TAG, "->myPictureCallback->onPictureTaken Error accessing image file: " + e.getMessage()); }
            }
        };

        myCameraErrorCallback = new Camera.ErrorCallback() {

            public void onError (int error, Camera camera) {

                Toast.makeText(CameraActivity.this, "Failed to take photo! Error Code: " + error, Toast.LENGTH_LONG).show();

                Log.e (TAG, "->myCameraErrorCallback Error " + error + " on camera object: " + camera);

                camera.startPreview();
            }
        };

        myMediaRecorderOnInfoListener = new MediaRecorder.OnInfoListener() {

            @Override
            public void onInfo (MediaRecorder recorder, int event, int extra) {

                if (event == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {

                    isRecording = false;

                    releaseMediaRecorder();

                    try { myCamera.reconnect(); } catch (Exception e) {

                        Toast.makeText (CameraActivity.this, "Failed to reconnect Camera!", Toast.LENGTH_LONG).show();

                        Log.e (TAG, "->myMediaRecorderOnInfoListener failed to reconnect Camera: " + e.getMessage());
                    }

                    myCamera.stopPreview();												// This magic line fixes the 'freeze after video recording stop' bug
                    // (See: https://code.google.com/p/android/issues/detail?id=52734)

                    try { myCamera.startPreview(); } catch (Exception e) {				// Restart preview updates

                        Toast.makeText (CameraActivity.this, "Failed to restart Camera preview!", Toast.LENGTH_LONG).show();

                        Log.e (TAG, "->myMediaRecorderOnInfoListener failed to restart Camera preview: " + e.getMessage());
                    }

                    btnRecordVideo.setText ("RECORD VIDEO");

                    Toast.makeText (CameraActivity.this, "Maximum video file size reached - Recording stopped", Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    Button.OnClickListener goRecButtonOnClickListener = new Button.OnClickListener() {

        @Override
        public void onClick (View v) {

            Log.i (TAG, "Entering takePhotoButtonOnClickListener");

            setContentView (R.layout.activity_main);

            //myCamera.takePicture (null, null, myPictureCallback);					// Take photo and callback

            //myCamera.startPreview();												// Restart preview updates
        }
    };

    Button.OnClickListener recordVideoButtonOnClickListener = new Button.OnClickListener() {

        @Override
        public void onClick (View v) {

            if (isRecording) {														// If we are currently recording:

                isRecording = false;												// Reset recording in progress flag

                myMediaRecorder.stop();												// Stop current recording in progress

                try { myCamera.reconnect(); } catch (Exception e) {					// Reconnect and re-lock access to the Camera

                    Toast.makeText (CameraActivity.this, "Failed to reconnect Camera!", Toast.LENGTH_LONG).show();

                    Log.e (TAG, "->recordVideoButtonOnClickListener failed to reconnect Camera: " + e.getMessage());
                }

                myCamera.stopPreview();												// This magic line fixes the 'freeze after video recording stop' bug
                // (See: https://code.google.com/p/android/issues/detail?id=52734)

                try { myCamera.startPreview(); } catch (Exception e) {				// Restart preview updates

                    Toast.makeText (CameraActivity.this, "Failed to restart Camera preview!", Toast.LENGTH_LONG).show();

                    Log.e (TAG, "->recordVideoButtonOnClickListener failed to restart Camera preview: " + e.getMessage());
                }

                btnRecordVideo.setText ("RECORD VIDEO");
            }

            else {																	// We are not currently recording:

                myCamera.unlock();													// Unlock camera to allow access by the Media process

                if (!prepareMediaRecorder()) {										// If we failed to prepare the audio/video/output settings for recording:

                    releaseMediaRecorder();											// Reset and release video recorder object

                    Toast.makeText (CameraActivity.this, "Failed to prepare video recorder!", Toast.LENGTH_LONG).show();

                    try { myCamera.reconnect(); } catch (Exception e) {				// Reconnect and re-lock access to the camera

                        Toast.makeText (CameraActivity.this, "Failed to reconnect Camera!", Toast.LENGTH_LONG).show();

                        Log.e (TAG, "->recordVideoButtonOnClickListener failed to reconnect Camera: " + e.getMessage());
                    }

                    return;															// Return to photo preview state, with isRecording flag still false
                }

                isRecording = true;													// Otherwise, set recording in progress flag

                myMediaRecorder.start();											// And start recording video

                btnRecordVideo.setText ("STOP RECORDING");
            }
        }
    };

    private void setupCamera() {

        try { myCamera = Camera.open(); }

        catch (Exception e) {

            Toast.makeText (CameraActivity.this, "Failed to setup Camera", Toast.LENGTH_LONG).show();

            Log.e (TAG, "->setupCamera: " + e.getMessage());

            return;
        }

        myCamera.setErrorCallback (myCameraErrorCallback);

        myCameraSurfaceView = new MyCameraSurfaceView (CameraActivity.this, myCamera);

        myCameraPreviewFrameLayout = (FrameLayout) findViewById (R.id.cameraPreview);

        myCameraPreviewFrameLayout.addView (myCameraSurfaceView);
    }

    private boolean prepareMediaRecorder() {

        myMediaRecorder = new MediaRecorder();

        myMediaRecorder.setOnInfoListener	(myMediaRecorderOnInfoListener);

        myMediaRecorder.setCamera			(myCamera);

        myMediaRecorder.setAudioSource		(MediaRecorder.AudioSource.CAMCORDER);

        myMediaRecorder.setVideoSource		(MediaRecorder.VideoSource.CAMERA);

        myMediaRecorder.setProfile			(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        myMediaRecorder.setOutputFile		(getOutputMediaFile (MEDIA_TYPE_VIDEO).getAbsolutePath());

        myMediaRecorder.setMaxFileSize		(5000000);

        myMediaRecorder.setPreviewDisplay	(myCameraSurfaceView.getHolder().getSurface());

        try { myMediaRecorder.prepare(); }

        catch (Exception e) { return false; }										// Caller handles exceptions

        return true;
    }

    @Override
    protected void onPause() {

        super.onPause();

        releaseMediaRecorder();														// Release MediaRecorder first

        releaseCamera();

        isRecording = false;														// Reset recording in progress flag
    }

    protected void onResume() {

        super.onResume();

        setupCamera();

        myCamera.startPreview();
    }

    private void releaseMediaRecorder() {

        if (myMediaRecorder != null) {

            myMediaRecorder.reset();												// Clear recorder configuration
            myMediaRecorder.release();												// Release the recorder object
            myMediaRecorder = null;
        }
    }

    private void releaseCamera() {

        if (myCamera != null) {

            myCamera.release();														// Release the camera for other applications
            myCamera = null;
        }
    }

    private static Uri getOutputMediaFileUri (int type) { return Uri.fromFile (getOutputMediaFile (type)); }	// Create a file Uri for saving an image or video

    private static File getOutputMediaFile (int type) {								// Create a File for saving an image or video

//TODO: Check that the SDCard is mounted using Environment.getExternalStorageState() before doing this.

        File mediaFile;

        File mediaStorageDir = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCamera");

// This location works best if you want the created images to be shared between applications and persist after your app has been uninstalled.

// Create the storage directory if it doesn't exist

        if (!mediaStorageDir.exists()) {

            if (!mediaStorageDir.mkdirs()) {

                Log.d (TAG, "->getOutputMediaFile Failed to create media storage directory");

                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format (new Date());

        if		(type == MEDIA_TYPE_IMAGE) { mediaFile = new File (mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg"); }

        else if (type == MEDIA_TYPE_VIDEO) { mediaFile = new File (mediaStorageDir.getPath() + File.separator + "VID_"+ timeStamp + ".mp4"); }

        else { return null; }

        return mediaFile;
    }
}
