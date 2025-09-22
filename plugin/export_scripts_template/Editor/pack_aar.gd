class_name pack_aar extends Node

static var warning:Array[int]
static var Errors:Array[int]

enum PackAARError {
	OK = 0,                           # Success
	REMOVE_TEMP_DIR_FAILED=50,           # Failed to remove temporary directory
	CREATE_TEMP_DIR_FAILED,           # Failed to create temporary directory
	EXTRACT_AAR_FAILED,               # Failed to extract AAR archive
	CREATE_DRAWABLE_DIR_FAILED,       # Failed to create drawable directory
	COPY_PICTURE_FAILED,              # Failed to copy picture into drawable
	WALLPAPER_XML_NOT_FOUND,          # wallpaper.xml is missing
	VALUES_XML_NOT_FOUND,             # values.xml is missing
	PACK_AAR_FAILED,                  # Failed while packing .aar
	OPEN_ZIP_FAILED,                  # Could not open .zip for reading
	NULL_DIRECTORY_ACCESS,            # Failed to open directory in packer
	FILE_OPEN_FAILED,                 # Failed to open a file for reading/writing
	PACKER_WRITE_FAILED,              # Failed to write file into packer
	XML_VALUE_MODIFY_FAILED           # Failed to modigy xml values
}

const PackAARErrorDescriptions := {
	PackAARError.OK: "Operation completed successfully.",
	PackAARError.REMOVE_TEMP_DIR_FAILED: "Failed to remove temporary directory.",
	PackAARError.CREATE_TEMP_DIR_FAILED: "Failed to create temporary directory.",
	PackAARError.EXTRACT_AAR_FAILED: "Could not extract the AAR archive.",
	PackAARError.CREATE_DRAWABLE_DIR_FAILED: "Failed to create res/drawable directory.",
	PackAARError.COPY_PICTURE_FAILED: "Failed to copy picture into drawable directory.",
	PackAARError.WALLPAPER_XML_NOT_FOUND: "wallpaper.xml not found in the extracted AAR.",
	PackAARError.VALUES_XML_NOT_FOUND: "values.xml not found in the extracted AAR.",
	PackAARError.PACK_AAR_FAILED: "Failed while repacking the AAR archive.",
	PackAARError.OPEN_ZIP_FAILED: "Could not open ZIP archive for reading.",
	PackAARError.NULL_DIRECTORY_ACCESS: "Could not open directory during packing.",
	PackAARError.FILE_OPEN_FAILED: "Failed to open a file for reading or writing.",
	PackAARError.PACKER_WRITE_FAILED: "Failed to write file into the packed AAR.",
	PackAARError.XML_VALUE_MODIFY_FAILED: "Failed to modify xml values."
}

static func readable_error(err:int)->String:
	if err<50:
		return error_string(err)
	return PackAARErrorDescriptions[err]
	
static func er_check(er:int)->int:
	if er!=OK:
		Errors.push_back(er)
	return er


static var temp_file_path: String :
	get:
		return OS.get_cache_dir()+"/TEMP/aar_packer"

static var wp_metadata_path:String:
	get:
		return "user://wp_metadata.dat"

static func modify_aar(aar_path: String, picture_path: String, new_description: String,author:String,contextUri:String,is_showinfo:bool, output_path: String) -> int:
	warning.clear()
	Errors.clear()
	if DirAccess.make_dir_recursive_absolute(temp_file_path) != OK:
		warning.push_back(PackAARError.CREATE_TEMP_DIR_FAILED)
	#print(temp_file_path)
	var res = extract_all_from_zip(aar_path,temp_file_path)
	if res != PackAARError.OK:
		return PackAARError.EXTRACT_AAR_FAILED
		
	var drawable_dir = temp_file_path + "/res/drawable"
	delete_files_in_dir(drawable_dir)
	var picture_name = picture_path.get_file()
	var target_pic = drawable_dir + "/" + picture_name
	if er_check(DirAccess.copy_absolute(ProjectSettings.globalize_path(picture_path), target_pic)) != OK:
		return er_check(PackAARError.COPY_PICTURE_FAILED)
	
	var xml_path = temp_file_path + "/res/xml/wallpaper.xml"
	if FileAccess.file_exists(xml_path):
		var text := FileAccess.get_file_as_string(xml_path)
		
		var regex = RegEx.new()
		regex.compile('android:thumbnail="@drawable/([^"]+)"')
		text = regex.sub(text, 'android:thumbnail="@drawable/' + picture_name.get_basename() + '"', true)
		
		regex.compile('android:showMetadataInPreview="([^"]+)"')
		text = regex.sub(text, 'android:showMetadataInPreview="' + str(is_showinfo) + '"')
		
		var f = FileAccess.open(xml_path, FileAccess.WRITE)
		if f == null:
			return er_check(FileAccess.get_open_error())
		if f.store_string(text) == false:
			return er_check(PackAARError.XML_VALUE_MODIFY_FAILED)
		f.close()
	else:
		return er_check(PackAARError.WALLPAPER_XML_NOT_FOUND)
	
	var values_path = temp_file_path + "/res/values/values.xml"
	if FileAccess.file_exists(values_path):
		var text := FileAccess.get_file_as_string(values_path)
		var regex = RegEx.new()
		regex.compile(r'(<string\s+name="wallpaper_description">)(.*?)(</string>)')
		text = regex.sub(text, "$1" + new_description + "$3", true)
		
		regex.compile(r'(<string\s+name="author">)(.*?)(</string>)')
		text = regex.sub(text, "$1" + author + "$3", true)
		regex.compile(r'(<string\s+name="contextUri">)(.*?)(</string>)')
		text = regex.sub(text, "$1" + contextUri + "$3", true)
		
		var f = FileAccess.open(values_path, FileAccess.WRITE)
		if f.store_string(text) == false:
			return er_check(PackAARError.XML_VALUE_MODIFY_FAILED)
		f.close()
	else:
		return er_check(PackAARError.VALUES_XML_NOT_FOUND)
	
	var packer = ZIPPacker.new()
	if er_check(packer.open(output_path,ZIPPacker.APPEND_CREATE)) == OK:
		_add_folder_to_packer(packer,temp_file_path,temp_file_path)
		packer.close()
	else:
		return er_check(PackAARError.OPEN_ZIP_FAILED)
	
	#save dock data after success
	var data_to_save = [picture_path, new_description ,author,contextUri,is_showinfo]
	var file = FileAccess.open(wp_metadata_path, FileAccess.WRITE)
	if file:
		file.store_var(data_to_save)  # store the whole array as a Variant
		file.close()
	return PackAARError.OK


static func extract_all_from_zip(fpath:String,dist_path:String)->int:
	var reader = ZIPReader.new()
	if er_check(reader.open(fpath)) != OK:
		return er_check(PackAARError.OPEN_ZIP_FAILED)
	var root_dir = DirAccess.open(dist_path)
	if root_dir==null:
		er_check(DirAccess.get_open_error())
		return er_check(PackAARError.EXTRACT_AAR_FAILED)
	var files = reader.get_files()
	for file_path in files:
		# If the current entry is a directory.
		if file_path.ends_with("/"):
			root_dir.make_dir_recursive(file_path)
			continue
		root_dir.make_dir_recursive(root_dir.get_current_dir().path_join(file_path).get_base_dir())
		var file = FileAccess.open(root_dir.get_current_dir().path_join(file_path), FileAccess.WRITE)
		if file == null:
			er_check(FileAccess.get_open_error())
			return er_check(PackAARError.FILE_OPEN_FAILED)
		var buffer = reader.read_file(file_path)
		file.store_buffer(buffer)
	return PackAARError.OK;



# Helper: recursively add files under folder_root into the packer.
# `folder_root` is the absolute root we want to map to the zip root (files inside zip will be paths relative to folder_root)
static func _add_folder_to_packer(packer:ZIPPacker, folder_path: String, folder_root: String) -> int:
	var dir := DirAccess.open(folder_path)
	if dir == null:
		er_check(DirAccess.get_open_error())
		return er_check(PackAARError.NULL_DIRECTORY_ACCESS)
	dir.list_dir_begin() # skip hidden? (flags don't matter much)
	var name := dir.get_next()
	while name != "":
		if name in [".", "..","dock.dat"]:
			name = dir.get_next()
			continue
		var abs_path := folder_path + "/" + name
		if dir.current_is_dir():
			var em:=_add_folder_to_packer(packer, abs_path, folder_root)
			if  em != PackAARError.OK:
				return er_check(em)
		else:
			# compute relative path inside zip
			var rel := abs_path.replace(folder_root + "/", "")
			# ensure zip uses forward slashes
			rel = rel.replace("\\", "/")
			var f := FileAccess.open(abs_path, FileAccess.READ)
			if f == null:
				er_check(FileAccess.get_open_error())
				return er_check(PackAARError.FILE_OPEN_FAILED)
			var bytes := f.get_buffer(f.get_length())
			f.close()
			# add to packer
			packer.start_file(rel)
			if packer.write_file(bytes) != OK:
				return er_check(PackAARError.PACKER_WRITE_FAILED)
		er_check(packer.close_file())
		name = dir.get_next()
	dir.list_dir_end()
	return PackAARError.OK

static func delete_files_in_dir(path: String) -> int:
	var dir := DirAccess.open(path)
	if dir == null:
		return er_check(DirAccess.get_open_error())
	dir.list_dir_begin()
	var file_name := dir.get_next()
	while file_name != "":
		if not dir.current_is_dir():
			var file_path := path.path_join(file_name)
			var err := dir.remove(file_path)
			if err != OK:
				er_check(err)
		# If it's a subdirectory, skip it — this only deletes files
		file_name = dir.get_next()
	dir.list_dir_end()
	return OK

static func load_wp_metadata_data()->Variant:
	var file = FileAccess.open(wp_metadata_path, FileAccess.READ)
	if file:
		var loaded_data = file.get_var()
		file.close()
		return loaded_data
	return null
