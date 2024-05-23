# Godot Android Live Wallpaper
Make your Godot Android application run in the baackground as a live wallpaper. Created from [`Android Plugin Template`](https://github.com/m4gr3d/Godot-Android-Plugin-Template). Also see [`Creating Android plugins`](https://docs.godotengine.org/en/4.0/tutorials/platform/android/android_plugin.html) for more details about godot plugins for android. To download the release, ready to use plugin files go to the [`Release section`](https://github.com/TheOathMan/Godot-Android-Live-Wallpaper/releases). Ro learn more about Godot Android Live Wallpaper, read further. 

## Building the plugin
- To build the plugin yourself, open the terminal in the project's root directory, and run this command:
```
./gradlew assemble
```
- Static debug and release libraries will be compiled into `addons/LiveWallpaper` folder. With that, you can copy the addons folder into your Godot project folder, and enable the plugin.

## How to setup
* Make sure the addons folder plugin is at `res://addons/`.
* Go to Project -> Project settings -> Plugins.
* Enable `LiveWallpaper`.
* Go to Project then click Reload Current Project.
* Now add `LiveWallpaper` node using the plus add-node button in the scene tab.

## How to use
Once the `LiveWallpaper` node has been added to the scene. You can:
* reference it from any script and start live wallpaper service by calling `start_live_wallpaper_service()` method
```
$LiveWallpaper.start_live_wallpaper_service()
```
* Connect to its signals by clicking connect from the Node tab. For example, connecting to signal visibility_changed(visibility:bool) to receive visibility updates, or from code as follows

```
$LiveWallpaper.visibility_changed.connect(is_user_viewing_my_wallpaper)
```

