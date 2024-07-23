@echo off
setlocal enabledelayedexpansion


REM Check if both source and destination paths are provided
if "%~1"=="" (
    echo Usage: rn.bat "wallpaper_name" "path_to_thumbnail"
    exit /b 1
)

if "%~2"=="" (
    echo Usage: rn.bat "wallpaper_name" "path_to_thumbnail"
    exit /b 1
)


set "inputfile=scripts\str_xml"
set "outputfile=str_xml_tmp"
set "distfile=plugin\src\main\res\values\strings.xml"

set "search=target_str"
set "replace=%~1"

if exist %outputfile% del %outputfile%

for /f "delims=" %%i in (%inputfile%) do (
    set "line=%%i"
    set "modified=!line:%search%=%replace%!"
    echo !modified! >> %outputfile%
)

rem Replace the original file with the modified one
move /y %outputfile% %distfile%


REM Extract the file name without the path
set "filePath=%~2"
for %%F in ("%filePath%") do set "fileName=%%~nF"


set "inputfile=scripts\pic_path_xml"
set "outputfile=pic_path_xml_tmp"
set "distfile=plugin\src\main\res\xml\wallpaper.xml"

if exist %outputfile% del %outputfile%

for /f "delims=" %%i in (%inputfile%) do (
    set "line=%%i"
    set "modified=!line:%search%=%fileName%!"
    echo !modified! >> %outputfile%
)

rem Replace the original file with the modified one
move /y %outputfile% %distfile%

REM Move pic to resource file
set "path_to_thumbnail=plugin\src\main\res\drawable"
call scripts/move_file.bat %filePath% %path_to_thumbnail%

call "./gradlew" assemble

echo Replacement complete.

endlocal