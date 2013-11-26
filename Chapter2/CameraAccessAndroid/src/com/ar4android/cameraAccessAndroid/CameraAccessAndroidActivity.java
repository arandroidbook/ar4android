/* CameraAccessAndroidAcvtivity - CameraAccessAndroid Example
 * 
 * Example Chapter 2
 * accompanying the book
 * "Augmented Reality for Android Application Development", Packt Publishing, 2013.
 * 
 * Copyright © 2013 Jens Grubert, Raphael Grasset / Packt Publishing.
 * 
 */
package com.ar4android.cameraAccessAndroid;

import java.util.List;

import android.hardware.Camera;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

public class CameraAccessAndroidActivity extends Activity {

	private static final String TAG = "CameraAccessAndroidActivity";

	private Camera mCamera;
	private CameraPreview mPreview;
	private int mDesiredCameraPreviewWidth = 640;  

	// Retrieve an instance of the Camera object.
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			// get a Camera instance
			c = Camera.open(0);
		} catch (Exception e) {
			Log.d(TAG, "Camera not available or in use.");			
		}
		// return NULL if camera is unavailable, otherwise return the Camera
		// instance
		return c;
	}
		
	// configure camera parameters like preview size
	private void initializeCameraParameters() {
		Camera.Parameters parameters = mCamera.getParameters();
		// Get a list of supported preview sizes.
		List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
		int currentWidth = 0;
		int currentHeight = 0;
		boolean foundDesiredWidth = false;
		for(Camera.Size s: sizes)
		{   if (s.width == mDesiredCameraPreviewWidth)  
		{
			currentWidth = s.width;
			currentHeight = s.height;
			foundDesiredWidth = true;
			break;
		}
		}
		if(foundDesiredWidth) {
			parameters.setPreviewSize( currentWidth, currentHeight);
		}    
		mCamera.setParameters(parameters);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public void onResume() {
		super.onResume();
		// Create an instance of Camera
		mCamera = getCameraInstance();		
		// initialize camera parameters
		initializeCameraParameters();        
		if(mCamera == null) {
			Log.d(TAG, "Camera not available");
		} else {
			// Create our Preview view and set it as the content of our activity.
			mPreview = new CameraPreview(this, mCamera);
			setContentView(mPreview);
		}
	}
	@Override
	protected void onPause() {
		super.onPause();        
		releaseCamera();              // release the camera immediately on pause event
	}

	private void releaseCamera(){
		if (mCamera != null){
			mCamera.stopPreview();
			mCamera.release();        // release the camera for other applications
			mCamera = null;
		}
	}
}
