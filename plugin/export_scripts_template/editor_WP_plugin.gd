@tool
extends EditorPlugin

const PLUGIN_NAME: String = "LiveWallpaper"
const NODE_NAME:String="LocationAndroid"
#//const PLUGIN_PACKAGE: String = "org.godotengine.plugin.android.LiveWallpaper"

var exportPlugin : AndroidExportPlugin

func _enter_tree() -> void:
	add_custom_type(NODE_NAME, "Node", preload("LiveWallpaper.gd"), preload("wp.svg"))
	exportPlugin = AndroidExportPlugin.new()
	add_export_plugin(exportPlugin)

func _exit_tree() -> void:
	remove_custom_type(NODE_NAME)
	remove_export_plugin(exportPlugin)
	exportPlugin=null

class AndroidExportPlugin extends EditorExportPlugin:

	func _supports_platform(platform: EditorExportPlatform) -> bool:
		if platform is EditorExportPlatformAndroid:
			return true
		return false
		
	func _get_android_libraries(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		if debug:
			return PackedStringArray(["LiveWallpaper/bin/debug/LiveWallpaper-debug.aar"])
		else:
			return PackedStringArray(["LiveWallpaper/bin/release/LiveWallpaper-release.aar"])
	
	func _get_name() -> String:
		return PLUGIN_NAME
