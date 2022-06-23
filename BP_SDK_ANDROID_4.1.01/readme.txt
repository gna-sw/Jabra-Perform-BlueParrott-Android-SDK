BlueParrott SDK v4.1.01

Included in zip :
- blueparrottsdk-release  - .aar file to import into your Android Studio project
- blueparrottsdkdemo - basic demo that shows how to connect to sdk, enable the sdk, listen for button events and update config settings on headset
- blueparrottsdkconnectsample - Sample application that includes code for automatically connecting and disconnecting from SDK when headset/bluetooth connects/disconnects
- blueparrottsdkaudiodemo - Sample application that includes a simple record/playback loop, when the Parrott button is connected
- javadocs  - java reference documentation

New in 4.1.00
- No changes to SDK interface
- SDK will now prioritise connection attempts to connect to BlueParrott headsets first, where more than one Bluetooth headset is connected
- targetSDK of demo apps updated to 30



New in 4.1.01
- Minimum SDK 23
- SDK Faster connections on Samsung/Android 12
- BPAudioDemo - file permission change on Android 12
- SDKConnectSample - simpler method to auto connect