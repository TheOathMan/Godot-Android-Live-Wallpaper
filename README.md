# Godot Android Location Plugin
Godot Android Plugin for Location Service. Retrieve user coordinates and handle client interactions seamlessly. based on Godot [`Android Plugin Template`](https://github.com/m4gr3d/Godot-Android-Plugin-Template). Also see [`Creating Android plugins`](https://docs.godotengine.org/en/4.0/tutorials/platform/android/android_plugin.html). To download the release, ready to use files go to the [`Release section`](https://github.com/TheOathMan/Godot-Android-Location-Plugin/releases).

## Building the plugin
- To build the plugin yourself, open the terminal in the project's root directory, and run this command:
```
./gradlew assemble
```
- Static debug and release libraries will be compiled into `addons/AndroidLocationPlugin` folder. With that, you can copy the addons folder into your Godot project folder, and enable the plugin.

## How to setup
* Make sure the addons folder plugin is at `res://addons`.
* Go to Project -> Project settings -> Plugins.
* Enable `LocationAndroid`.
* Go to Project then click Reload Current Project.
* Now add `LocationAndroid` node using the plus add-node button in the scene tab.

## How to use
Once the `LocationAndroid` node has been added to the scene. You can:
* reference it from any script and start location service by calling `begin_Android_location_service()` method
```
$LocationAndroid.begin_Android_location_service()
```
* Connect to its signals by clicking connect from the Node tab. For example, connecting to location_updated(Latitude:float,Longitude:float) to receive location updates, or from code as follows

```
$LocationAndroid.location_updated.connect(update_pin_position)
```

