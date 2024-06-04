class_name LiveWallpaper
extends Node

signal visibility_changed(visibility:bool)
signal apply_window_insets(L:int,R:int,U:int,D:int)
signal trim_memory(level:int)

# Start the wallpaper service
func start_live_wallpaper_service():
	get_plugin().startWallpaperService()

# IS this wallpaper service process running as a preview ?
func is_preview()-> bool:
	return get_plugin().IsPreview()

# Return true if this app is set as a live wallpaper for lock screen, system or for both.
func is_wallpaper_in_use()->bool:
	return get_plugin().isLiveWallpaperInUse()

# Reset wallpaper to the system defult 
func reset_to_default_Wallpaper()->void:
	get_plugin().ResetToDefaultWallpaper()

# return true if this app is running as a live wallpaper service.
func is_live_wallpaper()->bool:
	return get_plugin().IsLiveWallpaper()


enum TrimMemory {
	COMPLETE         = 80, # The system is experiencing a severe memory shortage and might start killing processes.  
	MODERATE         = 60, # Memory is getting tight, and the system suggests freeing up as much memory as possible. 
	BACKGROUND       = 40, # Memory is low, and background processes are strong candidates for termination.
	UI_HIDDEN        = 20, # Memory is low and UI is no longer visible.
	RUNNING_CRITICAL = 15, # System is in a critical memory state and might even terminate foreground processes.
	RUNNING_LOW      = 10, # Memory is running low for foreground processes.
	RUNNING_MODERATE = 5   # Similar to TRIM_MEMORY_MODERATE but applies to foreground processes.
}

func _ready():
	if Engine.has_singleton("LiveWallpaper"):
		_live_wallpaper_plugin = Engine.get_singleton("LiveWallpaper")
		_live_wallpaper_plugin.connect("VisibilityChanged",_visibility_changed)
		_live_wallpaper_plugin.connect("TrimMemory",_trim_memory)
		_live_wallpaper_plugin.connect("ApplyWindowInsets",_apply_window_insets)
	else:
		printerr("Failed to initialization Android live wallpaper Plugin")

#=============== internals ============ 
var _live_wallpaper_plugin

func get_plugin():
	if OS.get_name() != "Android":
		printerr("Wrong operating system for LiveWallpaper Plugin")
		return dummy;
	return _live_wallpaper_plugin

func _visibility_changed(visibility:bool)->void:
	visibility_changed.emit(visibility)

func _trim_memory(level:TrimMemory)->void:
	trim_memory.emit(level)

func _apply_window_insets(L:int,R:int,U:int,D:int)->void:
	apply_window_insets.emit(L,R,U,D)
	print(L,", ",R,", ",U," ,",D)

class dummy:
	static func startWallpaperService():pass
	static func IsPreview()-> bool:return false
	static func isLiveWallpaperInUse()->bool:return false
	static func ResetToDefaultWallpaper()->void:pass
	static func IsLiveWallpaper()->bool:return false

