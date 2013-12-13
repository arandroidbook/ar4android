/* RayPickingJMEActivity - RayPickingJME Example
 * 
 * Example Chapter 6
 * accompanying the book
 * "Augmented Reality for Android Application Development", Packt Publishing, 2013.
 * 
 * Copyright © 2013 Jens Grubert, Raphael Grasset / Packt Publishing.
 * 
 * This example is dependent of the Qualcomm Vuforia SDK 
 * The Vuforia SDK is a product of Qualcomm Austria Research Center GmbH
 * 
 * https://developer.vuforia.com
 * 
 * This example was built from the ImageTarget example accompanying the Vuforia SDK
 * https://developer.vuforia.com/resources/sample-apps/image-targets-sample-app
 * 
 */

package com.ar4android.rayPickingJME;

import com.jme3.app.AndroidHarness;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

import com.jme3.system.android.AndroidConfigChooser.ConfigType;
import com.jme3.texture.Image;
import com.qualcomm.QCAR.QCAR;

public class RayPickingJMEActivity extends AndroidHarness {

	private static final String TAG = "RayPickingJMEActivity";
	
    // Focus mode constants:
    private static final int FOCUS_MODE_NORMAL = 0;
    private static final int FOCUS_MODE_CONTINUOUS_AUTO = 1;

    // Application status constants:
    private static final int APPSTATUS_UNINITED         = -1;
    private static final int APPSTATUS_INIT_APP         = 0;
    private static final int APPSTATUS_INIT_QCAR        = 1;
    private static final int APPSTATUS_INIT_TRACKER     = 2;
    private static final int APPSTATUS_INIT_APP_AR      = 3;
    private static final int APPSTATUS_LOAD_TRACKER     = 4;
    private static final int APPSTATUS_INITED           = 5;
    private static final int APPSTATUS_CAMERA_STOPPED   = 6;
    private static final int APPSTATUS_CAMERA_RUNNING   = 7;

    // Name of the native dynamic libraries to load:
    private static final String NATIVE_LIB_SAMPLE = "VuforiaNative";
    private static final String NATIVE_LIB_QCAR = "QCAR";
    
    // Display size of the device:
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;

    // Constant representing invalid screen orientation to trigger a query:
    private static final int INVALID_SCREEN_ROTATION = -1;

    // Last detected screen rotation:
    private int mLastScreenRotation = INVALID_SCREEN_ROTATION;

    // The current application status:
    private int mAppStatus = APPSTATUS_UNINITED;

    // The async tasks to initialize the QCAR SDK:
    private InitQCARTask mInitQCARTask;
    private LoadTrackerTask mLoadTrackerTask;

    // An object used for synchronizing QCAR initialization, dataset loading and
    // the Android onDestroy() life cycle event. If the application is destroyed
    // while a data set is still being loaded, then we wait for the loading
    // operation to finish before shutting down QCAR:
    private Object mShutdownLock = new Object();

    // QCAR initialization flags:
    private int mQCARFlags = 0;

    // Contextual Menu Options for Camera Flash - Autofocus
    private boolean mFlash = false;
    private boolean mContAutofocus = false;

    // The menu item for swapping data sets:
    MenuItem mDataSetMenuItem = null;
    boolean mIsStonesAndChipsDataSetActive  = false;
    
    private RelativeLayout mUILayout;

    
    /** A helper for loading native libraries stored in "libs/armeabi*". */
    public static boolean loadLibrary(String nLibName)
    {
        try
        {
            System.loadLibrary(nLibName);
            Log.d(TAG, "Native library lib" + nLibName + ".so loaded");
            return true;
        }
        catch (UnsatisfiedLinkError ulee)
        {
            Log.d(TAG, "The library lib" + nLibName +
                            ".so could not be loaded");
        }
        catch (SecurityException se)
        {
        	Log.d(TAG, "The library lib" + nLibName +
                            ".so was not allowed to be loaded");
        }

        return false;
    }

   
    /** Static initializer block to load native libraries on start-up. */
    static
    {
        loadLibrary(NATIVE_LIB_QCAR);
        loadLibrary(NATIVE_LIB_SAMPLE);
    }
    
    /** An async task to initialize QCAR asynchronously. */
    private class InitQCARTask extends AsyncTask<Void, Integer, Boolean>
    {
        // Initialize with invalid value:
        private int mProgressValue = -1;

        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (mShutdownLock)
            {
                QCAR.setInitParameters(RayPickingJMEActivity.this, mQCARFlags);

                do
                {
                    // QCAR.init() blocks until an initialization step is
                    // complete, then it proceeds to the next step and reports
                    // progress in percents (0 ... 100%).
                    // If QCAR.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    mProgressValue = QCAR.init();

                    // Publish the progress value:
                    publishProgress(mProgressValue);

                    // We check whether the task has been canceled in the
                    // meantime (by calling AsyncTask.cancel(true)).
                    // and bail out if it has, thus stopping this thread.
                    // This is necessary as the AsyncTask will run to completion
                    // regardless of the status of the component that
                    // started is.
                } while (!isCancelled() && mProgressValue >= 0
                         && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }

        protected void onPostExecute(Boolean result)
        {
            // Done initializing QCAR, proceed to next application
            // initialization status:
            if (result)
            {
                Log.d(TAG,"InitQCARTask::onPostExecute: QCAR " +
                              "initialization successful");

                updateApplicationStatus(APPSTATUS_INIT_TRACKER);
            }
            else
            {
            	Log.d(TAG,"InitQCARTask::onPostExecute: QCAR " +
                    "initialization failed");
            }
        }
    }

    /** An async task to load the tracker data asynchronously. */
    private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap:
            synchronized (mShutdownLock)
            {
                // Load the tracker data set:
                return (loadTrackerData() > 0);
            }
        }

        protected void onPostExecute(Boolean result)
        {
            Log.d(TAG,"LoadTrackerTask::onPostExecute: execution " +
                        (result ? "successful" : "failed"));

            if (result)
            {
                // The stones and chips data set is now active:
                mIsStonesAndChipsDataSetActive = true;

                // Done loading the tracker, update application status:
                updateApplicationStatus(APPSTATUS_INITED);
            }
            else
            {
            	
            }
        }
    }


    /** Stores screen dimensions */
    private void storeScreenDimensions()
    {
        // Query display dimensions:
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }
    
    /** Configure QCAR with the desired version of OpenGL ES. */
    private int getInitializationFlags()
    {
        int flags = 0;

        flags = QCAR.GL_20;
       
        return flags;
    }
    
    private void updateActivityOrientation()
    {
        Configuration config = getResources().getConfiguration();

        boolean isPortrait = false;

        switch (config.orientation)
        {
        case Configuration.ORIENTATION_PORTRAIT:
            isPortrait = true;
            break;
        case Configuration.ORIENTATION_LANDSCAPE:
            isPortrait = false;
            break;
        case Configuration.ORIENTATION_UNDEFINED:
        default:
            break;
        }

        Log.d(TAG,"Activity is in "
                + (isPortrait ? "PORTRAIT" : "LANDSCAPE"));
        setActivityPortraitMode(isPortrait);
    }


    /**
     * Updates projection matrix and viewport after a screen rotation
     * change was detected.
     */
    public void updateRenderView()
    {
        int currentScreenRotation = getWindowManager().getDefaultDisplay().getRotation();
        if (currentScreenRotation != mLastScreenRotation)
        {
            // Set projection matrix if there is already a valid one:
            if (QCAR.isInitialized() && (mAppStatus == APPSTATUS_CAMERA_RUNNING))
            {
                Log.d(TAG,"RayPickingJMEActivity::updateRenderView");

                // Query display dimensions:
                storeScreenDimensions();

                // Update viewport via renderer:
                //TODO SET Screen here
               // mRenderer.updateRendering(mScreenWidth, mScreenHeight);

                // Update projection matrix:
                setProjectionMatrix();

                // Cache last rotation used for setting projection matrix:
                mLastScreenRotation = currentScreenRotation;
            }
        }
    }

    
    /** NOTE: this method is synchronized because of a potential concurrent
     * access by RayPickingJME::onResume() and InitQCARTask::onPostExecute(). */
    private synchronized void updateApplicationStatus(int appStatus)
    {
        // Exit if there is no change in status:
        if (mAppStatus == appStatus)
            return;

        // Store new status value:
        mAppStatus = appStatus;

        // Execute application state-specific actions:
        switch (mAppStatus)
        {
            case APPSTATUS_INIT_APP:
                // Initialize application elements that do not rely on QCAR
                // initialization:
                initApplication();

                // Proceed to next application initialization status:
                updateApplicationStatus(APPSTATUS_INIT_QCAR);
                break;

            case APPSTATUS_INIT_QCAR:
                // Initialize QCAR SDK asynchronously to avoid blocking the
                // main (UI) thread.
                //
                // NOTE: This task instance must be created and invoked on the
                // UI thread and it can be executed only once!
                try
                {
                    mInitQCARTask = new InitQCARTask();
                    mInitQCARTask.execute();
                }
                catch (Exception e)
                {
                   Log.d(TAG,"Initializing QCAR SDK failed");
                }
                break;

            case APPSTATUS_INIT_TRACKER:
                // Initialize the ImageTracker:
                if (initTracker() > 0)
                {
                    // Proceed to next application initialization status:
                    updateApplicationStatus(APPSTATUS_INIT_APP_AR);
                }
                break;

            case APPSTATUS_INIT_APP_AR:
                // Initialize Augmented Reality-specific application elements
                // that may rely on the fact that the QCAR SDK has been
                // already initialized:
                initApplicationAR();

                // Proceed to next application initialization status:
                updateApplicationStatus(APPSTATUS_LOAD_TRACKER);
                break;

            case APPSTATUS_LOAD_TRACKER:
                // Load the tracking data set:
                //
                // NOTE: This task instance must be created and invoked on the
                // UI thread and it can be executed only once!
                try
                {
                    mLoadTrackerTask = new LoadTrackerTask();
                    mLoadTrackerTask.execute();
                }
                catch (Exception e)
                {
                    Log.d(TAG,"Loading tracking data set failed");
                }
                break;

            case APPSTATUS_INITED:
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector:
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc();

                // Native post initialization:
                onQCARInitializedNative();

                // Activate the renderer:
               // mRenderer.mIsActive = true;

                // Now add the GL surface view. It is important
                // that the OpenGL ES surface view gets added
                // BEFORE the camera is started and video
                // background is configured.
                
              //  addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
              //          LayoutParams.MATCH_PARENT));

                // Sets the UILayout to be drawn in front of the camera
              //  mUILayout.bringToFront();

                // Start the camera:
                updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);

                break;

            case APPSTATUS_CAMERA_STOPPED:
                // Call the native function to stop the camera:
                stopCamera();
                break;

            case APPSTATUS_CAMERA_RUNNING:
                // Call the native function to start the camera:
                startCamera();

                // Sets the layout background to transparent
            //   mUILayout.setBackgroundColor(Color.TRANSPARENT);
                

                // Set continuous auto-focus if supported by the device,
                // otherwise default back to regular auto-focus mode.
                // This will be activated by a tap to the screen in this
                // application.
                if (!setFocusMode(FOCUS_MODE_CONTINUOUS_AUTO))
                {
                    mContAutofocus = false;
                    setFocusMode(FOCUS_MODE_NORMAL);
                }
                else
                {
                    mContAutofocus = true;
                }
                break;

            default:
                throw new RuntimeException("Invalid application state");
        }
    }

    /** Initialize application GUI elements that are not related to AR. */
    private void initApplication()
    {
        // Set the screen orientation:
        // NOTE: Use SCREEN_ORIENTATION_LANDSCAPE or SCREEN_ORIENTATION_PORTRAIT
        //       to lock the screen orientation for this activity.
        int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

        // This is necessary for enabling AutoRotation in the Augmented View
        if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
        {
            // NOTE: We use reflection here to see if the current platform
            // supports the full sensor mode (available only on Gingerbread
            // and above.
            try
            {
            	
                // SCREEN_ORIENTATION_FULL_SENSOR is required to allow all 
                // 4 screen rotations if API level >= 9:
                Field fullSensorField = ActivityInfo.class
                        .getField("SCREEN_ORIENTATION_FULL_SENSOR");
                screenOrientation = fullSensorField.getInt(null);
            }
            catch (NoSuchFieldException e)
            {
                // App is running on API level < 9, do nothing.
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // Apply screen orientation
        setRequestedOrientation(screenOrientation);

        updateActivityOrientation();

        // Query display dimensions:
        storeScreenDimensions();

        // As long as this window is visible to the user, keep the device's
        // screen turned on and bright:
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /** Initializes AR application components. */
    private void initApplicationAR()
    {
        // Do application initialization in native code (e.g. registering
        // callbacks, etc.):
        initApplicationNative(mScreenWidth, mScreenHeight);

        /*
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = QCAR.requiresAlpha();

      
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.activity_main,
                null, false);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        */
    }


    /** Native tracker initialization and deinitialization. */
    public native int initTracker();
    public native void deinitTracker();

    /** Native functions to load and destroy tracking data. */
    public native int loadTrackerData();
    public native void destroyTrackerData();

    /** Native sample initialization. */
    public native void onQCARInitializedNative();

    /** Native methods for starting and stopping the camera. */
    private native void startCamera();
    private native void stopCamera();

    /** Native method for setting / updating the projection matrix
     * for AR content rendering */
    private native void setProjectionMatrix();
    
    /** Native function to initialize the application. */
    private native void initApplicationNative(int width, int height);
    
    /** Native function to deinitialize the application.*/
    private native void deinitApplicationNative();    
    
    /** Tells native code whether we are in portait or landscape mode */
    private native void setActivityPortraitMode(boolean isPortrait);

    /** Tells native code to switch dataset as soon as possible*/
    private native void switchDatasetAsap();

    private native boolean autofocus();
    private native boolean setFocusMode(int mode);

    /** Activates the Flash */
    private native boolean activateFlash(boolean flash);

	private boolean mCreatedBefore = false;
	
	public RayPickingJMEActivity() {
		// Set the application class to run
		appClass = "com.ar4android.rayPickingJME.RayPickingJME";
		// Try ConfigType.FASTEST; or ConfigType.LEGACY if you have problems
		eglConfigType = ConfigType.BEST;
		// Exit Dialog title & message
		exitDialogTitle = "Exit?";
		exitDialogMessage = "Press Yes";
		// Enable verbose logging
		eglConfigVerboseLogging = false;
		// Choose screen orientation
		screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		// Invert the MouseEvents X (default = true)
		mouseEventsInvertX = true;
		// Invert the MouseEvents Y (default = true)
		mouseEventsInvertY = true;
	}
	
	Image cameraJMEImageRGB565;
	private byte[] mPreviewBufferRGB656;
	java.nio.ByteBuffer mPreviewByteBufferRGB565;
	static boolean firstTimeGetImage=true;
	
	public void initializeImageBuffer(int width,int height)
	{
		int bufferSizeRGB565 = width * height * 2 + 4096;

		mPreviewBufferRGB656 = null;
		
		mPreviewBufferRGB656 = new byte[bufferSizeRGB565];
		
		mPreviewByteBufferRGB565 = ByteBuffer.allocateDirect(mPreviewBufferRGB656.length);
		cameraJMEImageRGB565 = new Image(Image.Format.RGB565, width,
				height, mPreviewByteBufferRGB565);
	
	}
	
	 public void setRGB565CameraImage(byte[] buffer, int width, int height)  {
		 
		    // Log.d(TAG,"Update Camera Image..");
		 	if (firstTimeGetImage) 
		 	{
		 		initializeImageBuffer(width,height);
		 		firstTimeGetImage=false;
		 	}

			mPreviewByteBufferRGB565.clear();
			mPreviewByteBufferRGB565.put(buffer);
			
			cameraJMEImageRGB565.setData(mPreviewByteBufferRGB565);
			
			if ((com.ar4android.rayPickingJME.RayPickingJME) app != null) {
				((com.ar4android.rayPickingJME.RayPickingJME) app)
						.setVideoBGTexture(cameraJMEImageRGB565);
			}		  
		  
		}
	 
	// We override AndroidHarness.onCreate() to be able to add the SurfaceView
	// needed for camera preview
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		//eglConfigType=ConfigType.BEST_TRANSLUCENT;
		super.onCreate(savedInstanceState);
		
        // Query the QCAR initialization flags:
        mQCARFlags = getInitializationFlags();

        // Update the application status to start initializing application:
        updateApplicationStatus(APPSTATUS_INIT_APP);
        
	}
	
	@Override
    public void onResume() {
    	super.onResume();
    	
    	// make sure the AndroidGLSurfaceView view is on top of the view
		// hierarchy
		//view.setZOrderOnTop(true);
		
        // QCAR-specific resume operation
        QCAR.onResume();

        // We may start the camera only if the QCAR SDK has already been
        // initialized
        if (mAppStatus == APPSTATUS_CAMERA_STOPPED)
        {
            updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
        }
        
        firstTimeGetImage=true;
	}

	@Override
	protected void onPause() {
		super.onPause();		
	

        if (mAppStatus == APPSTATUS_CAMERA_RUNNING)
        {
            updateApplicationStatus(APPSTATUS_CAMERA_STOPPED);
        }

        // Disable flash when paused
        if (mFlash)
        {
            mFlash = false;
            activateFlash(mFlash);
        }

        // QCAR-specific pause operation
        QCAR.onPause();
        
        firstTimeGetImage=true;
//        QCAR.onSurfaceCreated();
	}
	
	@Override
    protected void onDestroy()
    {
        super.onDestroy();
        
        // Cancel potentially running tasks
        if (mInitQCARTask != null &&
            mInitQCARTask.getStatus() != InitQCARTask.Status.FINISHED)
        {
            mInitQCARTask.cancel(true);
            mInitQCARTask = null;
        }

        if (mLoadTrackerTask != null &&
            mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
        {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }

        // Ensure that all asynchronous operations to initialize QCAR
        // and loading the tracker datasets do not overlap:
        synchronized (mShutdownLock) {

            // Do application deinitialization in native code:
            deinitApplicationNative();

            // Destroy the tracking data set:
            destroyTrackerData();

            // Deinit the tracker:
            deinitTracker();

            // Deinitialize QCAR SDK:
            QCAR.deinit();
        }

        System.gc();
        
    }
	
	
	@Override
    public void onConfigurationChanged(Configuration config)
    {
       // DebugLog.LOGD("RayBasedJMEActivity::onConfigurationChanged");
        super.onConfigurationChanged(config);

        updateActivityOrientation();

        storeScreenDimensions();

        // Invalidate screen rotation to trigger query upon next render call:
        mLastScreenRotation = INVALID_SCREEN_ROTATION;
    }


}
