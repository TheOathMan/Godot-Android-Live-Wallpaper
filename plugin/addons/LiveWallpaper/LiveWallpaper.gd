class_name LiveWallpaper
extends Node

signal visibility_changed(visibility:bool)
signal apply_window_insets(L:int,R:int,U:int,D:int)
signal trim_memory(level:int)

var _live_wallpaper_plugin

func get_plugin():
	if OS.get_name() != "Android":
		printerr("Wrong operating system for LiveWallpaper Plugin")
		return dummy;
	return _live_wallpaper_plugin

func _ready():
	if Engine.has_singleton("LiveWallpaper"):
		_live_wallpaper_plugin = Engine.get_singleton("LiveWallpaper")
		_live_wallpaper_plugin.connect("VisibilityChanged",_visibility_changed)
		_live_wallpaper_plugin.connect("TrimMemory",_trim_memory)
		_live_wallpaper_plugin.connect("ApplyWindowInsets",_apply_window_insets)
	else:
		printerr("Failed to initialization Android live wallpaper Plugin")

func start_live_wallpaper_service():
	get_plugin().startWallpaperService()

func is_preview()-> bool:
	return get_plugin().IsPreview()

func is_wallpaper_in_use()->bool:
	return get_plugin().isLiveWallpaperInUse()

func reset_to_default_Wallpaper()->void:
	get_plugin().ResetToDefaultWallpaper()


#=============== internals ============ 

func _visibility_changed(visibility:bool):
	visibility_changed.emit(visibility)

func _trim_memory(level:int):
	trim_memory.emit(level)

func _apply_window_insets(L:int,R:int,U:int,D:int):
	apply_window_insets.emit(L,R,U,D)


class dummy:
	static func IsPreview()-> bool:return false
