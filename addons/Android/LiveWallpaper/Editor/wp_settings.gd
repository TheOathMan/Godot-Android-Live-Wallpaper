@tool
extends EditorPlugin

func _ready() -> void:
	var dd = pack_aar.load_wp_metadata_data()
	if dd:
		%wp_iconpath_edit.text=dd[0]
		%wp_name_edit.text=dd[1]
		%wp_author_edit.text=dd[2]
		%wp_contextUri.text=dd[3]
		%show_mt_cb.button_pressed=dd[4]

func _on_button_button_down(itr:int=0) -> void:
	%save_btn.disabled=true
	var build:="debug" if itr == 0 else "release"
	if itr == 0:
		%log.text=""
		%log.clear()
	
	var result = pack_aar.modify_aar("res://addons/Android/LiveWallpaper/bin/LiveWallpaper-%s.aar"%build,
		%wp_iconpath_edit.text, %wp_name_edit.text, %wp_author_edit.text, %wp_contextUri.text, %show_mt_cb.button_pressed,
		"res://addons/Android/LiveWallpaper/bin/LiveWallpaper-%s.aar"%build)
	
	%log.push_color(Color(0.825, 0.701, 0.0, 1.0)) # red
	for i in pack_aar.warning:
		%log.add_text("%s \n" % pack_aar.readable_error(i))
	%log.pop()
	
	if result == pack_aar.PackAARError.OK:
		%log.push_color(Color(0.0, 0.701, 0.166, 1.0) )
		%log.add_text("Changes to %s plugin file saved.\n" % build )
		%log.pop()
	else:
		%log.push_color(Color(0.912, 0.0, 0.08, 1.0))
		for i in pack_aar.Errors:
			%log.add_text("%s \n" %  pack_aar.readable_error(i))
		%log.pop()
	
	if itr==0:
		_on_button_button_down(1)
	pass # Replace with function body.

func _on_fileDialogue_button_button_up() -> void:
	$"../FileDialog".visible=true
	pass # Replace with function body.

func _on_file_dialog_file_selected(path: String) -> void:
	%wp_iconpath_edit.text = path
	%save_btn.disabled=false
	pass # Replace with function body.

func _on_wp_name_edit_text_changed(new_text: String) -> void:
	if new_text.is_empty():
		%save_btn.disabled=true
	else:
		%save_btn.disabled=false
	pass # Replace with function body.

func _on_wp_author_edit_text_changed(new_text: String) -> void:
	%save_btn.disabled=false
	pass # Replace with function body.

func _on_wp_iconpath_edit_text_changed(new_text: String) -> void:
	if new_text.is_empty():
		%save_btn.disabled=true
	else:
		%save_btn.disabled=false
	pass # Replace with function body.

func _on_show_mt_cb_toggled(toggled_on: bool) -> void:
	%save_btn.disabled=false
	pass # Replace with function body.
