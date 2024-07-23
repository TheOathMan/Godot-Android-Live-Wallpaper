@echo off

REM Check if both source and destination paths are provided
if "%~1"=="" (
    echo Usage: movefile.bat "source_file_path" "destination_directory_path"
    exit /b 1
)

if "%~2"=="" (
    echo Usage: movefile.bat "source_file_path" "destination_directory_path"
    exit /b 1
)

REM Get the source file path and destination directory path from arguments
set "sourceFile=%~1"
set "destinationDir=%~2"

REM Check if the source file exists
if not exist "%sourceFile%" (
    echo Source file does not exist: %sourceFile%
    exit /b 1
)

REM Check if the destination directory exists, create it if it does not
if not exist "%destinationDir%" (
    echo Destination directory does not exist, creating it: %destinationDir%
    mkdir "%destinationDir%"
)

REM Move the file
copy "%sourceFile%" "%destinationDir%"

REM Check if the move was successful
if errorlevel 1 (
    echo Failed to move the file: %sourceFile% to %destinationDir%
    exit /b 1
) else (
    echo File moved successfully: %sourceFile% to %destinationDir%
)


