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

# Set a static image as a secondary wallpaper in case another wallpaper process was created 
# while the device is using this app as a live wallpaper in the background.
func set_second_wallpaper_image(path)->void:
	get_plugin().SetSecondWallpaperImage(path)


func _ready():
	if Engine.has_singleton("LiveWallpaper"):
		_live_wallpaper_plugin = Engine.get_singleton("LiveWallpaper")
		_live_wallpaper_plugin.connect("VisibilityChanged",_visibility_changed)
		_live_wallpaper_plugin.connect("TrimMemory",_trim_memory)
		_live_wallpaper_plugin.connect("ApplyWindowInsets",_apply_window_insets)
	
	# [Optional] ========================================================
	# Because only one instance of the wallpaper engine process is allowed
	# to initialize the Godot engine, we can choose to show a still picture
	# of the main wallpaper in case the user tries to instantiate another
	# wallpaper instance for preview or through third-party wallpaper apps.
	# Here, I chose to take a screenshot of the main wallpaper only if an 
	# hour has passed and once at the visibility_changed call. You have the option
	# to delete or modify this behavior.
		visibility_changed.connect(
		func(v:bool):
			if(v==true):
				if(Time.get_ticks_msec() > _count_to_next_update):
					_count_to_next_update=Time.get_ticks_msec()+3600000
					var vp:Viewport = get_viewport()
					if(vp):
						vp.get_texture().get_image().save_png(_path_to_sec_WP)
						set_second_wallpaper_image(_path_to_sec_WP)
						)
	#====================================================================
	#====================================================================
	else:
		printerr("Failed to initialization Android live wallpaper Plugin")

#=============== internals ============ 
var _live_wallpaper_plugin
var _path_to_sec_WP:String = OS.get_user_data_dir().path_join("last_viewd_WP.png")
var _count_to_next_update:int=0

func get_plugin():
	if OS.get_name() != "Android":
		printerr("Wrong operating system for LiveWallpaper Plugin")
		return dummy;
	return _live_wallpaper_plugin

func _visibility_changed(visibility:bool)->void:
	visibility_changed.emit(visibility)

func _trim_memory(level:int)->void:
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

