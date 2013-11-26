/* LocationAccessJME - LocationAccessJME Example
 * 
 * Example Chapter 4
 * accompanying the book
 * "Augmented Reality for Android Application Development", Packt Publishing, 2013.
 * 
 * Copyright © 2013 Jens Grubert, Raphael Grasset / Packt Publishing.
 * 
 */

package com.ar4android.locationAccessJME;

import android.location.Location;
import android.util.Log;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class LocationAccessJME extends SimpleApplication  implements AnimEventListener { 

	private static final String TAG = "LocationAccessJME";
	// The geometry which will represent the video background
	private Geometry mVideoBGGeom;
	// The material which will be applied to the video background geometry.
	private Material mvideoBGMat;
	// The texture displaying the Android camera preview frames.
	private Texture2D mCameraTexture;
	// the JME image which serves as intermediate storage place for the Android
	// camera frame before the pixels get uploaded into the texture.
	private Image mCameraImage;
	// A flag indicating if the scene has been already initialized.
	private boolean mSceneInitialized = false;
	// A flag indicating if a new Android camera image is available.
	boolean mNewCameraFrameAvailable = false;	
	// A flag to indicate if the location has been updated once
	static boolean firstTimeLocation=true;

	//the User position which serves as intermediate storage place for the Android
	//Location listener position update
	private Vector3f mUserPosition;
	
	//A flag indicating if a new Location is available
	private boolean mNewUserPositionAvailable =false;
	
	private Vector3f mNinjaPosition;
	Location locationNinja;
	
	Camera fgCam;	
	Spatial ninja;
	private float mForegroundCamFOVY = 50; // for a Samsung Galaxy SII
	
	// for animation	
	// The controller allows access to the animation sequences of the model
	private AnimControl mAniControl;
	// the channel is used to run one animation sequence at a time
	private AnimChannel mAniChannel;
  
	public static void main(String[] args) {
		LocationAccessJME app = new LocationAccessJME();
		app.start();
	}

	@Override
	public void simpleInitApp() {
		// Do not display statistics
		setDisplayStatView(false);
		setDisplayFps(false);
		// we use our custom viewports - so the main viewport does not need the  rootNode
		viewPort.detachScene(rootNode);
		initVideoBackground(settings.getWidth(), settings.getHeight());
		initForegroundScene();			
		initBackgroundCamera();		
		initForegroundCamera(mForegroundCamFOVY);
	}

	// This function creates the geometry, the viewport and the virtual camera
	// needed for rendering the incoming Android camera frames in the scene
	// graph
	public void initVideoBackground(int screenWidth, int screenHeight) {
		// Create a Quad shape.
		Quad videoBGQuad = new Quad(1, 1, true);
		// Create a Geometry with the Quad shape
		mVideoBGGeom = new Geometry("quad", videoBGQuad);
		float newWidth = 1.f * screenWidth / screenHeight;
		// Center the Geometry in the middle of the screen.
		mVideoBGGeom.setLocalTranslation(-0.5f * newWidth, -0.5f, 0.f);//
		// Scale (stretch) the width of the Geometry to cover the whole screen
		// width.
		mVideoBGGeom.setLocalScale(1.f * newWidth, 1.f, 1);
		// Apply a unshaded material which we will use for texturing.
		mvideoBGMat = new Material(assetManager,
				"Common/MatDefs/Misc/Unshaded.j3md");
		mVideoBGGeom.setMaterial(mvideoBGMat);
		// Create a new texture which will hold the Android camera preview frame
		// pixels.
		mCameraTexture = new Texture2D();
		mUserPosition=new Vector3f();
		mNinjaPosition=new Vector3f();
		locationNinja=new Location("ninjaPOI");		
		mSceneInitialized = true;
	}
	
	public void initBackgroundCamera() {
		// Create a custom virtual camera with orthographic projection
		Camera videoBGCam = cam.clone();		
		videoBGCam.setParallelProjection(true);
		// Also create a custom viewport.
		ViewPort videoBGVP = renderManager.createMainView("VideoBGView",
				videoBGCam);
		// Attach the geometry representing the video background to the
		// viewport.
		videoBGVP.attachScene(mVideoBGGeom);
	}
	public void initForegroundScene() {
		// Load a model from test_data (OgreXML + material + texture)
        ninja = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        ninja.scale(0.05f, 0.05f, 0.05f);
        ninja.rotate(0.0f, -3.0f, 0.0f);
        ninja.setLocalTranslation(0.0f, -2.5f, -50.0f);
        rootNode.attachChild(ninja);
        
        // You must add a light to make the model visible
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        rootNode.addLight(sun);
	
        mAniControl = ninja.getControl(AnimControl.class);
        mAniControl.addListener(this);
        mAniChannel = mAniControl.createChannel();
        // show animation from beginning
        mAniChannel.setAnim("Walk");
        mAniChannel.setLoopMode(LoopMode.Loop);
        mAniChannel.setSpeed(1f);
	}
	
	public void initForegroundCamera(float fovY) {

		fgCam = new Camera(settings.getWidth(), settings.getHeight());
		fgCam.setLocation(new Vector3f(0f, 0f, 0f));
		fgCam.setAxes(new Vector3f(-1f,0f,0f), new Vector3f(0f,1f,0f), new Vector3f(0f,0f,-1f));
		fgCam.setFrustumPerspective(fovY,  settings.getWidth()/settings.getHeight(), 0.5f, 50000);
		ViewPort fgVP = renderManager.createMainView("ForegroundView", fgCam);
		fgVP.attachScene(rootNode);
		fgVP.setClearFlags(false, true, false);
		fgVP.setBackgroundColor(ColorRGBA.Blue);
	}

	 public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
		 // unused
	  }

	 public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
	    // unused
	  }
	 
	// This method retrieves the preview images from the Android world and puts
	// them into a JME image.
	public void setVideoBGTexture(final Image image) {
		if (!mSceneInitialized) {
			return;
		}
		mCameraImage = image;
		mNewCameraFrameAvailable = true;
	}
	
    private void WSG84toECEF(Location loc, Vector3f position) {
    	
    	double WGS84_A=6378137.0;           // WGS 84 semi-major axis constant in meters

    	double WGS84_E=0.081819190842622;   // WGS 84 eccentricity

    	double lat=(float) Math.toRadians(loc.getLatitude());
    	double lon=(float) Math.toRadians(loc.getLongitude());
    	
    	double clat=Math.cos(lat);
    	double slat=Math.sin(lat);
    	double clon=Math.cos(lon);
    	double slon=Math.sin(lon);

    	double N=WGS84_A / Math.sqrt(1.0 - WGS84_E * WGS84_E * slat * slat);

    	double x = N*clat*clon;
    	double y = N*clat*slon;
    	double z = (N * (1.0 - WGS84_E * WGS84_E)) * slat;
        
        position.set((float)x,(float)y,(float)z);
    }
    
    private void ECEFtoENU(Location loc, Vector3f cameraPosition, Vector3f poiPosition, Vector3f enuPOIPosition) {
    	double lat=(float) Math.toRadians(loc.getLatitude());
    	double lon=(float) Math.toRadians(loc.getLongitude());
    	
    	double clat=Math.cos(lat);
    	double slat=Math.sin(lat);
    	double clon=Math.cos(lon);
    	double slon=Math.sin(lon);

        double dx = cameraPosition.x - poiPosition.x;

        double dy = cameraPosition.y - poiPosition.y;

        double dz = cameraPosition.z - poiPosition.z;

        double e = -slon*dx  + clon*dy;

        double n = -slat*clon*dx - slat*slon*dy + clat*dz;

        double u = clat*clon*dx + clat*slon*dy + slat*dz;
        
        enuPOIPosition.set((float)e,(float)n,(float)u);
    }
    
	public void setUserLocation(Location location) {
		if (!mSceneInitialized) {
			return;
		}
		Log.d(TAG,"update camera position");
		WSG84toECEF(location,mUserPosition);
		
		
		if (firstTimeLocation) {
			//put it at 10 meters
			locationNinja.setLatitude(location.getLatitude()+0.0003);
			locationNinja.setLongitude(location.getLongitude());		
			firstTimeLocation=false;
		}
		
		Vector3f ECEFNinja=new Vector3f();
		Vector3f ENUNinja=new Vector3f();

		WSG84toECEF(locationNinja,ECEFNinja);

		ECEFtoENU(locationNinja,mUserPosition,ECEFNinja,ENUNinja);
		//x=east,y=north,z=up
		mNinjaPosition.set(ENUNinja.x,0,ENUNinja.y);
		Log.d(TAG,"Ninja Pos="+mNinjaPosition.toString());
		
		mNewUserPositionAvailable =true;
	}
 
	@Override
	public void simpleUpdate(float tpf) {
		if (mNewCameraFrameAvailable) {
			mCameraTexture.setImage(mCameraImage);
			mvideoBGMat.setTexture("ColorMap", mCameraTexture);
		}

		if (mNewUserPositionAvailable) {
			Log.d(TAG,"update user location");
			ninja.setLocalTranslation(/*mNinjaPosition.x+0.0f*/ 0 ,mNinjaPosition.y-2.5f,mNinjaPosition.z+0.0f);
			mNewUserPositionAvailable=false;
		}
		mVideoBGGeom.updateLogicalState(tpf);
		mVideoBGGeom.updateGeometricState();
	}

	@Override
	public void simpleRender(RenderManager rm) {
		// unused
	}
}
