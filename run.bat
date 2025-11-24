@echo off
setlocal enabledelayedexpansion

rem 创建编译目录
if not exist "build\classes" mkdir "build\classes"

rem 设置类路径
set "LWJGL_CP=."
if exist "lib\lwjgl" (
  set "LWJGL_CP=.;lib\lwjgl\*"
)

rem 查找所有 Java 源文件
set "SOURCES="
for /r "src\main\java" %%f in (*.java) do (
  set "SOURCES=!SOURCES! %%f"
)

rem 编译
echo Compiling Java sources...
javac -encoding UTF-8 -d build\classes -cp "%LWJGL_CP%" %SOURCES%
if errorlevel 1 (
  echo Compilation failed!
  exit /b 1
)
echo Compilation successful.

rem 设置运行时类路径
set "CLASSPATH=build\classes"
if exist "lib\lwjgl" (
  set "CLASSPATH=build\classes;lib\lwjgl\*"
)

rem 计算 natives 路径
set "OS_ID=windows"
if /i "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
  set "ARCH_ID=x86_64"
) else if /i "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
  set "ARCH_ID=arm64"
) else (
  set "ARCH_ID=x86_64"
)

rem 设置 LWJGL natives 路径
set "JAVA_FLAGS="
set "NATIVES_PATH=lib\lwjgl\natives\%OS_ID%-%ARCH_ID%"
if exist "%NATIVES_PATH%" (
  set "JAVA_FLAGS=-Dorg.lwjgl.librarypath=%NATIVES_PATH%"
)

rem 运行程序
echo Running game...
java %JAVA_FLAGS% -cp "%CLASSPATH%" com.gameengine.example.Game
