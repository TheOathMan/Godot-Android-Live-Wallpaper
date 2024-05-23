# Godot Android Live Wallpaper
Make your Godot Android application run in the background as a live wallpaper. Created from [`Android Plugin Template`](https://github.com/m4gr3d/Godot-Android-Plugin-Template). Also see [`Creating Android plugins`](https://docs.godotengine.org/en/4.0/tutorials/platform/android/android_plugin.html) for more details about godot plugins for android. To download the release, ready to use plugin files go to the [`Release section`](https://github.com/TheOathMan/Godot-Android-Live-Wallpaper/releases). To learn more about Godot Android Live Wallpaper, read more. 

## How to setup
* Make sure the addons folder plugin is at `res://addons/`.
* Go to Project -> Project settings -> Plugins.
* Enable `LiveWallpaper` plugin.
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

## Building the plugin
- To build the plugin yourself, open the terminal in the project's root directory, and run this command:
```
./gradlew assemble
```
- Static debug and release libraries will be compiled into `addons/LiveWallpaper` folder. With that, you can copy the addons folder into your Godot project folder, and enable the plugin.

## Important Considerations:

* This plugin is essentially a workaround for Godot's native Android code. While it has been tested with Godot versions 4.2.2.stable and 4.3.dev without any bugs or crashes, users with different devices might encounter issues.

* The plugin runs in the background, so be mindful of memory usage and power consumption once your app starts running as a live wallpaper. The plugin provides callback signals such as 'trim_memory' and 'visibility_changed'. Although the app pauses when it is no longer visible, ensure that you do not run intensive tasks when it becomes visible again.

* Only one instance of the live wallpaper is allowed to run at a time. Once you set the app as a live wallpaper, other instances of this process (live wallpaper process) will be blocked to prevent crashes. This does not mean you can't run the main app; you can do so without any issues. It means you can't run another instance of the live wallpaper, such as watching a preview of your live wallpaper while it is running as a service.