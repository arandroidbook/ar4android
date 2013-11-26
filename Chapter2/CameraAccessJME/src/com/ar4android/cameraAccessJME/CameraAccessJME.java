/* CameraAccessJME - CameraAccessJME Example
 * 
 * Example Chapter 2
 * accompanying the book
 * "Augmented Reality for Android Application Development", Packt Publishing, 2013.
 * 
 * Copyright © 2013 Jens Grubert, Raphael Grasset / Packt Publishing.
 * 
 */
package com.ar4android.cameraAccessJME;



import android.util.Log;

import com.jme3.app.SimpleApplication;

import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class CameraAccessJME extends SimpleApplication {

	private static final String TAG = "CameraAccessJME";
	// The geometry which will represent the video background
	private Geometry mVideoBGGeom;
	// The material which will be applied to the video background geometry.
	private Material mvideoBGMat;
	// The texture displaying the Android camera preview frames.
	private Texture2D mCameraTexture;
	// the JME image which serves as intermediate storage place for the Android camera frame before the pixels get uploaded into the texture.
	private Image mCameraImage;
	// A flag indicating if the scene has been already initialized.
	private boolean mSceneInitialized = false;
	// A flag indicating if a new Android camera image is available.
	boolean	mNewCameraFrameAvailable = false;

	public static void main(String[] args) {
		CameraAccessJME app = new CameraAccessJME();
		app.start();
	}

	// The default method used to initialize your JME application.
	@Override
	public void simpleInitApp() {
		// Do not display statistics or frames per second
		setDisplayStatView(false);
		setDisplayFps(false);
		// We use custom viewports - so the main viewport does not need to contain the rootNode
		viewPort.detachScene(rootNode);
		initVideoBackground(settings.getWidth(), settings.getHeight());
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
		// Scale (stretch) the width of the Geometry to cover the whole screen width.
		mVideoBGGeom.setLocalScale(1.f * newWidth, 1.f, 1);
		// Apply a unshaded material which we will use for texturing.
		mvideoBGMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mVideoBGGeom.setMaterial(mvideoBGMat);
		// Create a new texture which will hold the Android camera preview frame pixels.
		mCameraTexture = new Texture2D();
		
		// Create a custom virtual camera with orthographic projection
		Camera videoBGCam = cam.clone();
		videoBGCam.setParallelProjection(true);
		// Also create a custom viewport. 
		ViewPort videoBGVP = renderManager.createMainView("VideoBGView",
				videoBGCam);
		// Attach the geometry representing the video background to the viewport.
		videoBGVP.attachScene(mVideoBGGeom);
		mSceneInitialized = true;		
	}

	// This method retrieves the preview images from the Android world and puts them into a JME image.
	public void setTexture(final Image image) {
		if (!mSceneInitialized) {
			return;
		}
		mCameraImage = image;
		mNewCameraFrameAvailable = true;
	}

	// This method is called before every render pass.
	// Here we will update the JME texture if a new Android camera frame is available 
	@Override
	public void simpleUpdate(float tpf) {
		if(mNewCameraFrameAvailable) {
			mCameraTexture.setImage(mCameraImage);
			mvideoBGMat.setTexture("ColorMap", mCameraTexture);
			mNewCameraFrameAvailable = false;
		}
		// we have to update the video background node before the root node gets updated by the super class
		mVideoBGGeom.updateLogicalState(tpf);
		mVideoBGGeom.updateGeometricState();
	}

	
}
