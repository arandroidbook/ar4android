/* CameraPreview - SuperimposeJME Example
 * 
 * Example Chapter 3
 * accompanying the book
 * "Augmented Reality for Android Application Development", Packt Publishing, 2013.
 * 
 * Copyright © 2013 Jens Grubert, Raphael Grasset / Packt Publishing.
 */

package com.ar4android.superimposeJME;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
	private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.PreviewCallback mCameraPreviewCallback;

    public CameraPreview(Context context, Camera camera, Camera.PreviewCallback cameraCallback) {
        super(context);
        mCamera = camera;
        mCameraPreviewCallback=cameraCallback;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
          // preview surface does not exist
        	Log.e(TAG,"no preview surface (surfaceChanged)");
          return;
        }
       
        
        
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
        	if (mCamera ==null)
        	{
        		Log.e(TAG,"no camera in start preview");
        	}
        	else
        	{
        		mCamera.setPreviewCallback(mCameraPreviewCallback);
        		mCamera.setPreviewDisplay(mHolder);
        		mCamera.startPreview();
        	}
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}