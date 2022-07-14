BlueParrott SDK v4.3.01

Included in zip :
- blueparrottsdk-release  - .aar file to import into your Android Studio project
- blueparrottsdkdemo - basic demo that shows how to connect to sdk, enable the sdk, listen for button events and update config settings on headset
- blueparrottsdkconnectsample - Sample application that includes code for automatically connecting and disconnecting from SDK when headset/bluetooth connects/disconnects
- blueparrottsdkaudiodemo - Sample application that includes a simple record/playback loop, when the Parrott button is connected
- javadocs  - java reference documentation

New in 4.3.01
Target SDK Updated
- Library now targets android sdk 32 (targetSdkVersion 32)
- All sample apps (SDKDemo, SDKConnection, SDKAudio) also target sdk 32

SDK Checks for new Permissions
- SDK now checks for appropriate permissions based on Android Version
- Android 12 +requires BLUETOOTH_CONNECT always
- Android 12+ requires BLUETOOTH_SCAN if connecting over BLE
- Android 11 and before requires FINE_LOCATION if connecting over BLE

Sample Apps implement new Permissions
- All sample apps have new logic to check for appropriate permissions based on Android version
