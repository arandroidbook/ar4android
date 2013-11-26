ar4android
==========

Companion Code to the Book Augmented Reality for Android Application Development

*Introduction*
This directory contains Eclipse projects for the individual samples discussed in the book "
Augmented Reality for Android Application Development". They are ordered by chapter. For the examples in chapter 5 and 6 you need to install the Qualcomm Vuforia SDK first. Please refer to the instructions in ThirdParty\readme.txt.

*Eclipse Project Import*
To import an existing project into Eclipse, open the menu:
- File -> Import 
- In the import dialouge Choose Android -> Existing Android Code Into Workspace
- In the following "Import Projects" dialouge set the "Root Directory" by pressing the "Browse..." button. Make sure you import the projects on a per chapter basis (e.g., choose /ARprojects/Chapter2 as root directory).
- If you get error messages related to a missing Android platform, make sure to select a platform which is installed on your system. Check the setting under the menu Project --> Properties --> Android

*All examples*
- We highly recommend you to use physical devices for testing, not the emulator, as you need access to the physical camera. There are ways to use your webcame in the emulator, but we did not test our code on the emulator. 
- You exit all running examples by pressing the back button on your Android device and then pressing the Yes button int the Exit dialouge.
- to allow parallel installs of all examples we updated the package names from "com.ar4android" to "com.ar4android.appName". For example "appClass = "com.ar4android.CameraAccessJME";" becomes "appClass = "com.ar4android.cameraAccessJME.CameraAccessJME";"

*Example Chapter4/LocationAccessJME*
- Make sure you enable the Android location services on your device. See https://support.google.com/coordinate/answer/2569281?hl=en for instructions

*Examples Chapter4/SensorAccessJME and Chapter4/SensorFusionJME*
- For the examples to run out of the box we assume that the default orientation of the device has a x-axis pointing to the right, y-axis pointing up, z-axis pointing to the user when the device is held in portrait mode (default for most smartphones). Some devices 8such as tablets) have a different default orientation. In this case you need to remap the sensor coordinates to the screen coordinates as mentioned here: http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords
- In some degenerate configurations (specifically when the device lies flat on surface) the orientation sensors will not initialize properly and you will not be able to see the 3D object. Avoid to put the device flat on the ground at startup time.
