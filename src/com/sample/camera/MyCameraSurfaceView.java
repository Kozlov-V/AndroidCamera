package com.sample.camera;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

@SuppressLint ("ViewConstructor")
public class MyCameraSurfaceView extends SurfaceView implements	SurfaceHolder.Callback {

	public static final int		PREVIEW_WINDOW_WIDTH	= 800;
	public static final int		PREVIEW_WINDOW_HEIGHT	= 480;
    
	private static final String TAG = "camera->MyCameraSurfaceView";
	
	private Camera	mCamera;

	public MyCameraSurfaceView (Context context, Camera camera) {
		
		super (context);

		mCamera = camera;

		this.getHolder().addCallback (this);									// Get the SurfaceHolder providing access and control over this SurfaceView's underlying surface...
	}																			// ... and add this SurfaceHolder as the Callback for underlying surface created/changed/destroyed events

	@Override
	public void surfaceCreated (SurfaceHolder holder) {

		try { mCamera.setPreviewDisplay (holder); }								// Tell the camera to draw preview frames onto the SurfaceHolder (the object holding the SurfaceView)
		
		catch (IOException e) { Log.e (TAG, "->surfaceCreated had an error setting preview display: " + e.getMessage()); }
	}

	@Override
	public void surfaceChanged (SurfaceHolder holder, int format, int weight, int height) {

		if (holder.getSurface() == null) { return; }

		try {
			
			mCamera.stopPreview();
			
			Parameters parameters = mCamera.getParameters();
	        
	        parameters.setPreviewSize (PREVIEW_WINDOW_WIDTH, PREVIEW_WINDOW_HEIGHT);
	        
	        mCamera.setParameters (parameters);
	        
	        mCamera.startPreview();
		}

		catch (Exception e) { Log.e (TAG, "->surfaceChanged error: " + e.getMessage()); }
	}
	
	@Override
	public void surfaceDestroyed (SurfaceHolder holder) {
		
		holder.removeCallback (this);
	    
		mCamera = null;		
	}
}