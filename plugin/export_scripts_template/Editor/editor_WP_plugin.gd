@tool
extends EditorPlugin

const PLUGIN_NAME: String = "LiveWallpaper"
const NODE_NAME:String="LocationAndroid"
#//const PLUGIN_PACKAGE: String = "org.godotengine.plugin.android.LiveWallpaper"

var exportPlugin : AndroidExportPlugin
var dock

func _enter_tree() -> void:
	if RenderingServer.get_current_rendering_method() != "gl_compatibility":
		push_error("The live wallpaper plugin only works on compatibility (for now)")
	add_custom_type(NODE_NAME, "Node", preload("res://addons/Android/LiveWallpaper/LiveWallpaper.gd"), preload("wp.svg"))
	exportPlugin = AndroidExportPlugin.new()
	add_export_plugin(exportPlugin)
	
	dock = preload("res://addons/Android/LiveWallpaper/Editor/Wallpaper_settings.tscn").instantiate()
	# Apply the editor theme
	dock.theme = EditorInterface.get_editor_theme()
	add_control_to_dock(DOCK_SLOT_LEFT_BR, dock)


func _exit_tree() -> void:
	remove_custom_type(NODE_NAME)
	remove_export_plugin(exportPlugin)
	exportPlugin=null
	#dock
	remove_control_from_docks(dock)
	dock.free()

class AndroidExportPlugin extends EditorExportPlugin:
	func _export_begin(features: PackedStringArray, is_debug: bool, path: String, flags: int) -> void:
		print('export begin feature:',features,' mode:','debug' if is_debug else 'release',', path:',path,' flags int:',flags)
	
	func _supports_platform(platform: EditorExportPlatform) -> bool:
		if platform is EditorExportPlatformAndroid:
			return true
		return false
		
	func _get_android_libraries(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		if debug:
			return PackedStringArray(["res://addons/Android/LiveWallpaper/bin/LiveWallpaper-debug.aar"])
		else:
			return PackedStringArray(["res://addons/Android/LiveWallpaper/bin/LiveWallpaper-release.aar"])
	
	func _get_name() -> String:
		return PLUGIN_NAME
