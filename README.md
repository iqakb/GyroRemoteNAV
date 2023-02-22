# GyroRemoteNAV
Android application and ROS Node for gyroscope-based quadcopter remote control in MAVROS.

For Bayucaraka's Internship Final Project.

## How to install the application
1. Download and install the Android Studio program in your computer
2. Copy the "gyroapp" folder into your AndroidStudioProjects folder
3. Connect your phone through an USB cable
4. Allow debuging on your phone by enabling its developer options
5. Wait until your phone to be recognized by Android Studio
6. Click run and wait for the application to install on your phone
### Alternatively, install the APK file directly on your phone
1. Download the 'gyroapp-debug.apk' on your phone
2. Depending on your phone, you have to allow 'Unknown Sources' to install application outside from Google Play Store
3. Install the APK and ignore whatever warning your phone might throw at you

## How to use the app
1. First enter your address and port with whichever host you want to connect to the MAVROS then press the connect button. Make sure the inputted port matches the port in the tcpcommander.cpp file (It's 8888 by default)
2. Hold on the clutch and mantain your phone orientation until the quadcopter arms itself
3. Slide on the clutch to change the quadcopter linear x velocity. Tilt the phone to change the quadcopter linear z velocity. Roll the phone to change the quadcopter linear y velocity. Rotate the phone to change the quadcopter yaw rate.
4. Hold on the clutch to maintain the command, or let it go to reset the commands.

## How to install the ros package
1. Create a ROS workspace if there isn't any
2. Move the gyroreciever folder into the workspace src/ folder
3. Open the terminal in your workspace folder
4. Enter `catkin make` into the terminal
5. Enter `source devel/setup.bash` into the terminal
6. Enter `rosrun gyroreader tcpcommander` into the terminal
