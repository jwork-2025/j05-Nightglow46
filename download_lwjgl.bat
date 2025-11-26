@echo off
setlocal enabledelayedexpansion

set "LWJGL_VERSION=3.3.6"
set "MAVEN_REPO=https://repo1.maven.org/maven2/org/lwjgl"
set "LIB_DIR=lib\lwjgl"

rem 检测架构
set "OS=windows"
if /i "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
  set "ARCH=x86_64"
) else if /i "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
  set "ARCH=arm64"
) else (
  echo Unsupported ARCH: %PROCESSOR_ARCHITECTURE%
  exit /b 1
)
set "CLASSIFIER=%OS%-%ARCH%"

echo Detected classifier: %CLASSIFIER%

rem 创建目录
if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"

rem 定义所有需要下载的模块
set "MODULES=lwjgl lwjgl-glfw lwjgl-opengl lwjgl-stb"

rem 下载每个模块的 jar 和 natives
for %%M in (%MODULES%) do (
  set "MODULE=%%M"
  set "BASE=%MAVEN_REPO%/!MODULE!/%LWJGL_VERSION%/!MODULE!-%LWJGL_VERSION%"
  echo Downloading !MODULE!-%LWJGL_VERSION%.jar ...
  curl -L -o "%LIB_DIR%\!MODULE!-%LWJGL_VERSION%.jar" "!BASE!.jar"
  if errorlevel 1 (
    echo Failed to download !MODULE!-%LWJGL_VERSION%.jar
  )
  
  rem 尝试下载natives - windows使用windows而不是windows-x86_64
  echo Downloading !MODULE!-%LWJGL_VERSION%-natives-windows.jar ...
  curl -L -o "%LIB_DIR%\!MODULE!-%LWJGL_VERSION%-natives-windows.jar" "!BASE!-natives-windows.jar"
  if errorlevel 1 (
    echo Warning: Failed to download !MODULE!-%LWJGL_VERSION%-natives-windows.jar
  )
  echo.
)

rem 删除404错误文件（如果存在）
for %%F in ("%LIB_DIR%\*.jar") do (
  set "SIZE=0"
  for %%A in ("%%F") do set "SIZE=%%~zA"
  if !SIZE! LSS 1000 (
    echo Removing invalid file %%F ^(size: !SIZE! bytes^)
    del "%%F"
  )
)

echo Done downloading all modules. Please check files under %LIB_DIR%.
echo.
dir "%LIB_DIR%\*.jar"
echo.
pause
