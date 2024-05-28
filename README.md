# Godot Android Live Wallpaper
Make your Godot Android application run in the background as a live wallpaper. To download the release, ready to use plugin files go to the [`Release`](https://github.com/TheOathMan/Godot-Android-Live-Wallpaper/releases) section. To learn more about Godot Android Live Wallpaper, read more. 

This project template is from [`Android Plugin Template`](https://github.com/m4gr3d/Godot-Android-Plugin-Template). Also see [`Creating Android plugins`](https://docs.godotengine.org/en/4.0/tutorials/platform/android/android_plugin.html) for more details about godot plugins for android.

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
You might want to build the plugin yourself to set a wallpaper name and a wallpaper preview picture. To build the plugin:

- Open the terminal in the project's root directory, and run this command:
```
./gradlew assemble
```
- Static debug and release libraries will be compiled into `addons/LiveWallpaper/bin` folder. With that, you can copy the addons folder into your Godot project folder, and enable the plugin.

## Known Issues/Limitations
* You can't run two instances of the live wallpaper app that instantiate the Godot engine. So, you cannot set the wallpaper on your device and then open a wallpaper preview of the same wallpaper. This could happen through third-party live wallpaper apps or by calling start_live_wallpaper_service() again. However, the plugin allows you to set a still picture to be shown if another preview of your wallpaper is called from somewhere.

* Setting the app as a live wallpaper for the lock screen, home screen, or both will always set it to both. A workaround is to set it manually from the device wallpaper settings. At least, that's the behavior in Android 14.

## Important Considerations:

* The plugin will attempt to run your entire Godot application as a background live wallpaper, including handling all touch inputs. Therefore, ensure you query the is_live_wallpaper() function to delete or limit frame rate and free resources that are not essential to the live wallpaper process, such as UI elements. Check the sample project in the [`release`](https://github.com/TheOathMan/Godot-Android-Live-Wallpaper/releases) section for a working implementation of this.

* This plugin is essentially a workaround for Godot's native Android that expect Android Activity not Service. While the plugin has been tested with Godot versions 4.2.2.stable and 4.3.dev without any noticeable bugs or crashes, users with different devices might encounter issues.

* The plugin runs in the background, so be mindful of memory usage and power consumption once your app starts running as a live wallpaper. The plugin provides callback signals such as 'trim_memory' and 'visibility_changed'. Although the app pauses when it is no longer visible, ensure that you do not run intensive tasks when it becomes visible again.

* Only one instance of the live wallpaper is allowed to run at a time. Once you set the app as a live wallpaper, other instances of this process (live wallpaper process) will be blocked to prevent crashes. This does not mean you can't run the main app; you can do so without any issues as it's in a different process. It just means you can't run another instance of the live wallpaper, such as watching a preview of your live wallpaper app while it is running as a service in the background. In that case, you can call is_wallpaper_in_use() then tell the users its already running for example.
