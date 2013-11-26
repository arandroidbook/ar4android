/* SensorAccessJMEActivity - SensorAccessJME Example
 * 
 * Example Chapter 4
 * accompanying the book
 * "Augmented Reality for Android Application Development", Packt Publishing, 2013.
 * 
 * Copyright © 2013 Jens Grubert, Raphael Grasset / Packt Publishing.
 * 
 */

package com.ar4android.sensorAccessJME;

import com.jme3.app.AndroidHarness;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

//include packages for Android Location API
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.system.android.AndroidConfigChooser.ConfigType;
import com.jme3.texture.Image;

public class SensorAccessJMEActivity extends AndroidHarness {

	private static final String TAG = "SensorAccessJMEActivity";
	private Camera mCamera;
	private CameraPreview mPreview;
	private int mDesiredCameraPreviewWidth = 640;
	private byte[] mPreviewBufferRGB565 = null;
	java.nio.ByteBuffer mPreviewByteBufferRGB565;
	// the actual size of the preview images
	int mPreviewWidth;
	int mPreviewHeight;
	// If we have to convert the camera preview image into RGB565 or can use it
	// directly
	private boolean pixelFormatConversionNeeded = true;
	private boolean stopPreview = false;
	Image cameraJMEImageRGB565;
	
	private SensorManager sensorManager;
	Sensor rotationVectorSensor;
 	Sensor gyroscopeSensor;
 	Sensor magneticFieldSensor;
 	Sensor accelSensor;
 	Sensor linearAccelSensor;

    private SensorEventListener sensorListener = new SensorEventListener() {
    	
        final double NS2S = 1.0f / 1000000000.0f;
   
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
    		Log.d(TAG, "onAccuracyChanged: " + accuracy);		
			
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			 
			switch(event.sensor.getType()) {
    		case Sensor.TYPE_ACCELEROMETER:
    			//do something
    			break;	
    		case Sensor.TYPE_LINEAR_ACCELERATION:
    			//do something
    			break;
    		case Sensor.TYPE_MAGNETIC_FIELD:  
    			//do something
    			break;
    		case Sensor.TYPE_GYROSCOPE:
    			//do something
    			break;
    		case Sensor.TYPE_ROTATION_VECTOR:
    			float[] rotationVector= {event.values[0],event.values[1], event.values[2]};    
    			float[] quaternion = {0.f,0.f,0.f,0.f};
    			sensorManager.getQuaternionFromVector(quaternion,rotationVector);   
    			float qw = quaternion[0]; float qx = quaternion[1];
    			float qy = quaternion[2]; float qz = quaternion[3];    			
    			double headingQ = Math.atan2(2*qy*qw-2*qx*qz , 1 - 2*qy*qy - 2*qz*qz);
    			double pitchQ = Math.asin(2*qx*qy + 2*qz*qw); 
    			double rollQ = Math.atan2(2*qx*qw-2*qy*qz , 1 - 2*qx*qx - 2*qz*qz);
    			if ((com.ar4android.sensorAccessJME.SensorAccessJME) app != null) {   		
    				((com.ar4android.sensorAccessJME.SensorAccessJME) app).setRotation((float)pitchQ, (float)rollQ, (float)headingQ);    			
				}
    			break;	
		  }
		}
    };

    protected Sensor initSingleSensor( int type, String name ){
    	Sensor newSensor = sensorManager.getDefaultSensor(type);
		if(newSensor != null){
			if(sensorManager.registerListener(sensorListener, newSensor, SensorManager.SENSOR_DELAY_GAME)) {
				Log.i(TAG, name + " successfully registered default");
			} else {
				Log.e(TAG, name + " not registered default");
			}
		} else {
			List<Sensor> deviceSensors = sensorManager.getSensorList(type);
			if(deviceSensors.size() > 0){
				Sensor mySensor = deviceSensors.get(0);
				if(sensorManager.registerListener(sensorListener, mySensor, SensorManager.SENSOR_DELAY_GAME)) {
					Log.i(TAG, name + " successfully registered to " + mySensor.getName());
				} else {
					Log.e(TAG, name + " not registered to " + mySensor.getName());
				}
			} else {
				Log.e(TAG, "No " + name + " sensor!");
			}
		}
		return newSensor;
    }  
    
    protected void initSensors(){
		 // look specifically for the gyroscope first and then for the rotation_vector_sensor (underlying sensors vary from platform to platform)
		 gyroscopeSensor = initSingleSensor(Sensor.TYPE_GYROSCOPE, "TYPE_GYROSCOPE");
		 rotationVectorSensor = initSingleSensor(Sensor.TYPE_ROTATION_VECTOR, "TYPE_ROTATION_VECTOR");
		 accelSensor = initSingleSensor(Sensor.TYPE_ACCELEROMETER, "TYPE_ACCELEROMETER");
		 linearAccelSensor = initSingleSensor(Sensor.TYPE_LINEAR_ACCELERATION, "TYPE_LINEAR_ACCELERATION");
		 magneticFieldSensor = initSingleSensor(Sensor.TYPE_MAGNETIC_FIELD, "TYPE_MAGNETIC_FIELD");
    }	
    
	// Implement the interface for getting copies of preview frames
	private final Camera.PreviewCallback mCameraCallback = new Camera.PreviewCallback() {
		public void onPreviewFrame(byte[] data, Camera c) {
			if (c != null && stopPreview == false) {
				mPreviewByteBufferRGB565.clear();
				// Perform processing on the camera preview data.
				if (pixelFormatConversionNeeded) {
					yCbCrToRGB565(data, mPreviewWidth, mPreviewHeight,
							mPreviewBufferRGB565);
					mPreviewByteBufferRGB565.put(mPreviewBufferRGB565);
				} else {
					mPreviewByteBufferRGB565.put(data);
				}
				cameraJMEImageRGB565.setData(mPreviewByteBufferRGB565);
				if ((com.ar4android.sensorAccessJME.SensorAccessJME) app != null) {
					((com.ar4android.sensorAccessJME.SensorAccessJME) app)
							.setVideoBGTexture(cameraJMEImageRGB565);
				}
			}
		}
	};

	// Retrieve an instance of the Camera object.
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			// get a Camera instance
			c = Camera.open(0);
		} catch (Exception e) {
			// Camera is does not exist or is already in use
			Log.e(TAG, "Camera not available or in use.");
		}
		// return NULL if camera is unavailable, otherwise return the Camera
		// instance
		return c;
	}

	// configure camera parameters like preview size
	private void initializeCameraParameters() {
		Camera.Parameters parameters = mCamera.getParameters();
		// Get a list of supported preview sizes.
		List<Camera.Size> sizes = mCamera.getParameters()
				.getSupportedPreviewSizes();
		int currentWidth = 0;
		int currentHeight = 0;
		boolean foundDesiredWidth = false;
		for (Camera.Size s : sizes) {
			if (s.width == mDesiredCameraPreviewWidth) {
				currentWidth = s.width;
				currentHeight = s.height;
				foundDesiredWidth = true;
				break;
			}
		}
		if (foundDesiredWidth) {
			parameters.setPreviewSize(currentWidth, currentHeight);
		}
		// we also want to use RGB565 directly
		List<Integer> pixelFormats = parameters.getSupportedPreviewFormats();
		for (Integer format : pixelFormats) {
			if (format == ImageFormat.RGB_565) {
				Log.d(TAG, "Camera supports RGB_565");
				pixelFormatConversionNeeded = false;
				parameters.setPreviewFormat(format);
				break;
			}
		}
		if (pixelFormatConversionNeeded == true) {
			Log.e(TAG,
					"Camera does not support RGB565 directly. Need conversion");
		}
		mCamera.setParameters(parameters); 
	}

	public SensorAccessJMEActivity() {
		// Set the application class to run
		appClass = "com.ar4android.sensorAccessJME.SensorAccessJME";
		// Try ConfigType.FASTEST; or ConfigType.LEGACY if you have problems
		eglConfigType = ConfigType.BEST;
		// Exit Dialog title & message
		exitDialogTitle = "Exit?";
		exitDialogMessage = "Press Yes";
		// Enable verbose logging
		eglConfigVerboseLogging = false;
		// Choose screen orientation
		screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		mouseEventsInvertX = true;
		// Invert the MouseEvents Y (default = true)
		mouseEventsInvertY = true;
	}


	// We override AndroidHarness.onCreate() to be able to add the SurfaceView
	// needed for camera preview
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // sensor setup
       sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
		Log.d(TAG, "Integrated sensors:");
		for(int i = 0; i < deviceSensors.size(); ++i ) {
			Sensor curSensor = deviceSensors.get(i);
			Log.d(TAG, curSensor.getName() + "\t" + curSensor.getType() + "\t" + curSensor.getMinDelay() / 1000.0f);
		}
    	initSensors();
	}
	
    @Override
    public void onStop() {
    	super.onStop();
    	sensorManager.unregisterListener(sensorListener);
    }
	
	
	@Override
    public void onResume() {
    	super.onResume();
    	
    	stopPreview = false;
    	// make sure the AndroidGLSurfaceView view is on top of the view
		// hierarchy
		view.setZOrderOnTop(true);

		// Create an instance of Camera
		mCamera = getCameraInstance();
		// initialize camera parameters
		initializeCameraParameters();

		// register our callback function to get access to the camera preview
		// frames
		preparePreviewCallbackBuffer();
		
		if (mCamera == null) {
			Log.e(TAG, "Camera not available");
		} else {
			// Create our Preview view and set it as the content of our
			// activity.
			mPreview = new CameraPreview(this, mCamera, mCameraCallback);
			// We do not want to display the Camera Preview view at startup - so
			// we resize it to 1x1 pixel.
			ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(1, 1);
			addContentView(mPreview, lp);
		}
		
	}

	@Override
	protected void onPause() {
		stopPreview = true;
		super.onPause();		
		// Make sure to release the camera immediately on pause.
		releaseCamera();		
		// remove the SurfaceView
		ViewGroup parent = (ViewGroup) mPreview.getParent(); 
		parent.removeView(mPreview);
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			// Release the camera.
			mCamera.release();
			mCamera = null;
		}
	}

	// prepares the Camera preview callback buffers.
	public void preparePreviewCallbackBuffer() {		
		int pformat;
		pformat = mCamera.getParameters().getPreviewFormat();
		Log.e(TAG, "PREVIEW format: " + pformat);
		// Get pixel format information to compute buffer size.
		PixelFormat info = new PixelFormat();
		PixelFormat.getPixelFormatInfo(pformat, info);		
		// The actual preview width and height.
		// They can differ from the requested width mDesiredCameraPreviewWidth
		mPreviewWidth = mCamera.getParameters().getPreviewSize().width;
		mPreviewHeight = mCamera.getParameters().getPreviewSize().height;
		int bufferSizeRGB565 = mPreviewWidth * mPreviewHeight * 2 + 4096;
		//Delete buffer before creating a new one.
		mPreviewBufferRGB565 = null;		
		mPreviewBufferRGB565 = new byte[bufferSizeRGB565];
		mPreviewByteBufferRGB565 = ByteBuffer.allocateDirect(mPreviewBufferRGB565.length);
		cameraJMEImageRGB565 = new Image(Image.Format.RGB565, mPreviewWidth,
				mPreviewHeight, mPreviewByteBufferRGB565);
	}

	public static void yCbCrToRGB565(byte[] yuvs, int width, int height,
			byte[] rgbs) {

		// the end of the luminance data
		final int lumEnd = width * height;
		// points to the next luminance value pair
		int lumPtr = 0;
		// points to the next chromiance value pair
		int chrPtr = lumEnd;
		// points to the next byte output pair of RGB565 value
		int outPtr = 0;
		// the end of the current luminance scanline
		int lineEnd = width;

		while (true) {

			// skip back to the start of the chromiance values when necessary
			if (lumPtr == lineEnd) {
				if (lumPtr == lumEnd)
					break; // we've reached the end
				// division here is a bit expensive, but's only done once per
				// scanline
				chrPtr = lumEnd + ((lumPtr >> 1) / width) * width;
				lineEnd += width;
			}

			// read the luminance and chromiance values
			final int Y1 = yuvs[lumPtr++] & 0xff;
			final int Y2 = yuvs[lumPtr++] & 0xff;
			final int Cr = (yuvs[chrPtr++] & 0xff) - 128;
			final int Cb = (yuvs[chrPtr++] & 0xff) - 128;
			int R, G, B;

			// generate first RGB components
			B = Y1 + ((454 * Cb) >> 8);
			if (B < 0)
				B = 0;
			else if (B > 255)
				B = 255;
			G = Y1 - ((88 * Cb + 183 * Cr) >> 8);
			if (G < 0)
				G = 0;
			else if (G > 255)
				G = 255;
			R = Y1 + ((359 * Cr) >> 8);
			if (R < 0)
				R = 0;
			else if (R > 255)
				R = 255;
			// NOTE: this assume little-endian encoding
			rgbs[outPtr++] = (byte) (((G & 0x3c) << 3) | (B >> 3));
			rgbs[outPtr++] = (byte) ((R & 0xf8) | (G >> 5));

			// generate second RGB components
			B = Y2 + ((454 * Cb) >> 8);
			if (B < 0)
				B = 0;
			else if (B > 255)
				B = 255;
			G = Y2 - ((88 * Cb + 183 * Cr) >> 8);
			if (G < 0)
				G = 0;
			else if (G > 255)
				G = 255;
			R = Y2 + ((359 * Cr) >> 8);
			if (R < 0)
				R = 0;
			else if (R > 255)
				R = 255;
			// NOTE: this assume little-endian encoding
			rgbs[outPtr++] = (byte) (((G & 0x3c) << 3) | (B >> 3));
			rgbs[outPtr++] = (byte) ((R & 0xf8) | (G >> 5));
		}
	}

}
