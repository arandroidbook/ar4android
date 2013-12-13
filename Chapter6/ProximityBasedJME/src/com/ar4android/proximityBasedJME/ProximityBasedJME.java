/* ProximityBasedJME - ProximityBasedJME Example
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

package com.ar4android.proximityBasedJME;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import android.util.Log;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.input.FlyByCamera;
import com.jme3.input.KeyInput;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;


import com.jme3.math.Ray;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;

public class ProximityBasedJME extends SimpleApplication implements AnimEventListener  {

	private static final String TAG = "ProximityBasedJME";
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
	// A flag indicating if the JME Image has been already initialized.
	private boolean mVideoImageInitialized = false;
	// A flag indicating if a new Android camera image is available.
	boolean mNewCameraFrameAvailable = false;

//	private float mForegroundCamFOVY = 30;
	private float mForegroundCamFOVY = 50; // for a Samsung Galaxy SII
	
	// for animation	
	// The controller allows access to the animation sequences of the model
	private AnimControl mAniControl;
	// the channel is used to run one animation sequence at a time
	private AnimChannel mAniChannel;
  
	Camera videoBGCam;
	Camera fgCam;
	Node shootables;
	Geometry geom1;
	Geometry geom2;
	Geometry geom3;
	
    /** Native function for initializing the renderer. */
    public native void initTracking(int width, int height);

    /** Native function to update the renderer. */
    public native void updateTracking();

    
	public static void main(String[] args) {
		ProximityBasedJME app = new ProximityBasedJME();
		app.start();
	}

	// The default method used to initialize your JME application.
	@Override
	public void simpleInitApp() {
		Log.e(TAG, "simpleInitApp");
		
		// Do not display statistics or frames per second
		setDisplayStatView(false);
		setDisplayFps(false);
		
		// We use custom viewports - so the main viewport does not need to contain the rootNode
		viewPort.detachScene(rootNode);
		initTracking(settings.getWidth(), settings.getHeight());
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
		mVideoBGGeom.setLocalTranslation(-0.5f*newWidth,-0.5f,0.f);
		// Scale (stretch) the width of the Geometry to cover the whole screen
		// width.
		mVideoBGGeom.setLocalScale(newWidth, 1.f, 1.f);
		
		// Apply a unshaded material which we will use for texturing.
		mvideoBGMat = new Material(assetManager,
				"Common/MatDefs/Misc/Unshaded.j3md");
		mVideoBGGeom.setMaterial(mvideoBGMat);
		// Create a new texture which will hold the Android camera preview frame
		// pixels.
		mCameraTexture = new Texture2D();
	
		mSceneInitialized = true;	
	}

	public void initBackgroundCamera() {
		// Create a custom virtual camera with orthographic projection
		videoBGCam = new Camera(settings.getWidth(), settings.getHeight());
		videoBGCam.setViewPort(0.0f, 1.0f, 0.f, 1.0f);
		videoBGCam.setLocation(new Vector3f(0f, 0f, 1.f));		
		videoBGCam.setAxes(new Vector3f(-1f,0f,0f), new Vector3f(0f,1f,0f), new Vector3f(0f,0f,-1f));
		videoBGCam.setParallelProjection(true);
		
		// Also create a custom viewport.
		ViewPort videoBGVP = renderManager.createMainView("VideoBGView",
				videoBGCam);
		// Attach the geometry representing the video background to the
		// viewport.
		videoBGVP.attachScene(mVideoBGGeom);
	}
	
	public void initForegroundScene() {
		
		//use the box for debugging
				
        Box b = new Box(7.f,4.f,6.f); // create cube shape at the origin

        geom1 = new Geometry("Box", b);  // create cube geometry from the shape
        Material mat = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
        mat.setColor("Color", ColorRGBA.Red);   // set color of material to blue
        geom1.setMaterial(mat);                   // set the cube's material

        geom1.setLocalTranslation(new Vector3f(0.0f,0.0f,6.0f));
        
        rootNode.attachChild(geom1);              // make the cube appear in the scene
          
        //DEBUG: 3 AXIS CS
        //you can enable the code for debugging purpose
        /*
        Box bX = new Box(0.1f, 0.1f, 0.1f); // create cube shape at the origin
        Geometry geomBX = new Geometry("Box", bX);  // create cube geometry from the shape
        Material matBX = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
        matBX.setColor("Color", ColorRGBA.Red);   // set color of material to blue
        geomBX.setMaterial(matBX);                   // set the cube's material
      //  geomBX.setLocalTranslation(new Vector3f(10.0f,0.0f,0.0f));
        geomBX.setLocalTranslation(new Vector3f(28.0f/2.0f,0.0f,0.0f));
        rootNode.attachChild(geomBX);              // make the cube appear in the scene      
        Box bY = new Box(0.1f, 0.1f, 0.1f); // create cube shape at the origin
        Geometry geomBY = new Geometry("Box", bY);  // create cube geometry from the shape
        Material matBY = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
        matBY.setColor("Color", ColorRGBA.Green);   // set color of material to blue
        geomBY.setMaterial(matBY);                   // set the cube's material
        geomBY.setLocalTranslation(new Vector3f(0.0f,19.6f/2.0f,0.0f));
        rootNode.attachChild(geomBY);              // make the cube appear in the scene
        Box bZ = new Box(0.1f, 0.1f, 0.1f); // create cube shape at the origin
        Geometry geomBZ = new Geometry("Box", bZ);  // create cube geometry from the shape
        Material matBZ = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
        matBZ.setColor("Color", ColorRGBA.Blue);   // set color of material to blue
        geomBZ.setMaterial(matBZ);                   // set the cube's material
        geomBZ.setLocalTranslation(new Vector3f(0.0f,0.0f,10.0f));
        rootNode.attachChild(geomBZ);              // make the cube appear in the scene
        */
		
        Sphere s = new Sphere(12,12,6); // create sphere shape at the origin
        geom2 = new Geometry("Sphere", s);  // create sphere geometry from the shape
        Material mat2 = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
        mat2.setColor("Color", ColorRGBA.Green);   // set color of material to blue
        geom2.setMaterial(mat2);                   // set the sphere material

        geom2.setLocalTranslation(new Vector3f(0.0f,0.0f,6.0f));

        rootNode.attachChild(geom2);              // make the cube appear in the scene

        Torus t = new Torus(12, 12, 2, 6); // create torus shape at the origin
        geom3 = new Geometry("Torus", t);  // create torus geometry from the shape
        Material mat3 = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
        mat3.setColor("Color", ColorRGBA.Blue);   // set color of material to blue
        geom3.setMaterial(mat3);                   // set the torus material

        geom3.setLocalTranslation(new Vector3f(0.0f,0.0f,1.0f));
        
        rootNode.attachChild(geom3);              // make the cube appear in the scene

	}
	
	public void initForegroundCamera(float fovY) {

		fgCam = new Camera(settings.getWidth(), settings.getHeight());
		
		fgCam.setViewPort(0, 1.0f, 0.f,1.0f);
		fgCam.setLocation(new Vector3f(0f, 0f, 0f));
		fgCam.setAxes(new Vector3f(-1f,0f,0f), new Vector3f(0f,1f,0f), new Vector3f(0f,0f,-1f));
		fgCam.setFrustumPerspective(fovY,  settings.getWidth()/settings.getHeight(), 1, 1000);

		ViewPort fgVP = renderManager.createMainView("ForegroundView", fgCam);
		fgVP.attachScene(rootNode);
		//color,depth,stencil
		fgVP.setClearFlags(false, true, false);
		fgVP.setBackgroundColor(new ColorRGBA(0,0,0,1));
//		fgVP.setBackgroundColor(new ColorRGBA(0,0,0,0));
	}
	
	
	public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
		 // unused
	}

	public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
	    // unused
	  }
	 
	public void setCameraPerspectiveNative(float fovY,float aspectRatio) {
			Log.d(TAG,"Update Camera Perspective..");
			 			
			fgCam.setFrustumPerspective(fovY,aspectRatio,1.0f, 1000.0f);
	}

	public void setCameraPoseNative(float cam_x,float cam_y,float cam_z) {
		 Log.d(TAG,"Update Camera Pose..");
		 fgCam.setLocation(new Vector3f(cam_x,cam_y,cam_z));
	}
	
		
	public void setCameraViewportNative(float viewport_w,float viewport_h,float size_x,float size_y) {
		 Log.d(TAG,"Update Camera Viewport..");
		
		float newWidth = 1.f;
		float newHeight = 1.f;
		
		if (viewport_h != settings.getHeight())
		{
			newWidth=viewport_w/viewport_h;
			newHeight=1.0f;
			videoBGCam.resize((int)viewport_w,(int)viewport_h,true);
			videoBGCam.setParallelProjection(true);
		}
		//exercise: find the similar transformation 
		//when viewport_w != settings.getWidth
		
		//Adjusting viewport: from BackgroundTextureAccess example in Qualcomm Vuforia
	    float viewportPosition_x =  (((int)(settings.getWidth()  - viewport_w)) / (int) 2);//+0
	    float viewportPosition_y =  (((int)(settings.getHeight() - viewport_h)) / (int) 2);//+0
	    float viewportSize_x = viewport_w;//2560
	    float viewportSize_y = viewport_h;//1920

	    //transform in normalized coordinate
	    viewportPosition_x =  (float)viewportPosition_x/(float)viewport_w;
	    viewportPosition_y =  (float)viewportPosition_y/(float)viewport_h;
	    viewportSize_x = viewportSize_x/viewport_w;
	    viewportSize_y = viewportSize_y/viewport_h;
	       
		//adjust for viewport start (modify video quad)
		mVideoBGGeom.setLocalTranslation(-0.5f*newWidth+viewportPosition_x,-0.5f*newHeight+viewportPosition_y,0.f);
		//adust for viewport size (modify video quad)
		mVideoBGGeom.setLocalScale(newWidth, newHeight, 1.f);
	}
	
	public void setCameraOrientationNative(float cam_right_x,float cam_right_y,float cam_right_z,
			float cam_up_x,float cam_up_y,float cam_up_z,float cam_dir_x,float cam_dir_y,float cam_dir_z) {
		 
		 Log.d(TAG,"Update Orientation Pose..");

		 fgCam.setAxes(
				 	new Vector3f(-cam_right_x,-cam_right_y,-cam_right_z), 
			 		new Vector3f(-cam_up_x,-cam_up_y,-cam_up_z),
			 		new Vector3f(cam_dir_x,cam_dir_y,cam_dir_z));

		 }
	
		 
	// This method retrieves the preview images from the Android world and puts them into a JME image.
		public void setVideoBGTexture(final Image image) {
			if (!mSceneInitialized) {
				return;
			}
			mCameraImage = image;
			mNewCameraFrameAvailable = true;
		}	
		
		
		@Override
		public void simpleUpdate(float tpf) {
			
			updateTracking();
			if (mNewCameraFrameAvailable) {
				mCameraTexture.setImage(mCameraImage);
				mvideoBGMat.setTexture("ColorMap", mCameraTexture);
			}

//			mCubeGeom.rotate(new Quaternion(1.f, 0.f, 0.f, 0.01f));
			Vector3f pos=new Vector3f();
			
			pos=fgCam.getLocation();
			
			if (pos.length()>50.)
			{
		        rootNode.attachChild(geom1);           
		        rootNode.detachChild(geom2);       
		        rootNode.detachChild(geom3); 

			}
			else
				if (pos.length()>40.)
				{
			        rootNode.detachChild(geom1);           
			        rootNode.attachChild(geom2);       
			        rootNode.detachChild(geom3); 
				}
				else
				{	
			        rootNode.detachChild(geom1);           
			        rootNode.detachChild(geom2);       
			        rootNode.attachChild(geom3); 
				}
				 
			
			mVideoBGGeom.updateLogicalState(tpf);
			mVideoBGGeom.updateGeometricState();
		}

		@Override
		public void simpleRender(RenderManager rm) {
			// TODO: add render code
		}
	
}
