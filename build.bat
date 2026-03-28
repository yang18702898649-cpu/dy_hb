@echo off
chcp 65001 >nul
echo ==========================================
echo      抖音红包助手 - 构建脚本
echo ==========================================
echo.

:: 检查是否安装了Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Java环境，请先安装JDK
    pause
    exit /b 1
)

echo [1/4] 正在清理构建目录...
call gradlew clean
if errorlevel 1 (
    echo [错误] 清理失败
    pause
    exit /b 1
)

echo [2/4] 正在构建项目...
call gradlew build
if errorlevel 1 (
    echo [错误] 构建失败
    pause
    exit /b 1
)

echo [3/4] 正在打包APK...
call gradlew assembleDebug
if errorlevel 1 (
    echo [错误] 打包失败
    pause
    exit /b 1
)

echo [4/4] 构建完成！
echo.
echo APK文件位置:
echo   app\build\outputs\apk\debug\app-debug.apk
echo.
pause
