@echo off
setlocal

set ANDROID_NDK_HOME=D:\android-ndk-r26d
set SDK=C:\Program Files\Unity\Hub\Editor\2021.3.37f1c1\Editor\Data\PlaybackEngines\AndroidPlayer\SDK
set BT=%SDK%\build-tools\30.0.2
set JAR=%SDK%\platforms\android-30\android.jar
set JAVA=C:\Program Files\Java\jdk-22\bin
set DXJAR=%BT%\lib\dx.jar
set PROJECT=%~dp0

echo === [1/6] Building Rust .so (arm64-v8a) ===
cd /d "%PROJECT%phire-ui"
cargo ndk -t arm64-v8a -o ./android/app/src/main/jniLibs build --release
if errorlevel 1 goto :error

echo === [2/6] Copying .so to origin ===
copy /y "android\app\src\main\jniLibs\arm64-v8a\libphire_ui.so" "%PROJECT%origin\lib\arm64-v8a\libphire_ui.so"

echo === [3/6] Compiling Java ===
cd /d "%PROJECT%"
rmdir /s /q build_classes 2>nul
mkdir build_classes
for /r android\app\src\main\java %%f in (*.java) do echo %%f >> build_classes.txt
"%JAVA%\javac.exe" --release 8 -classpath "%JAR%" -d build_classes @build_classes.txt
if errorlevel 1 goto :error
del build_classes.txt

echo === [4/6] Generating DEX ===
"%JAVA%\jar.exe" cf build_classes.jar -C build_classes .
"%JAVA%\java.exe" -cp "%DXJAR%" com.android.dx.command.Main --dex --output=origin\classes.dex --min-sdk-version=21 build_classes.jar
if errorlevel 1 goto :error

echo === [5/6] Building manifest ===
"%BT%\aapt2.exe" compile android\app\src\main\res\values\strings.xml -o compiled_strings.zip
python -c "import re,xml.etree.ElementTree as ET; ET.register_namespace('android','http://schemas.android.com/apk/res/android'); t=ET.parse('android/app/src/main/AndroidManifest.xml'); t.getroot().attrib['{http://schemas.android.com/apk/res/android}package']='com.hlmc.phire'; t.write('origin/AndroidManifest.xml',xml_declaration=True,encoding='utf-8')"
"%BT%\aapt2.exe" link -o manifest_fixed.zip --manifest origin\AndroidManifest.xml -I "%JAR%" -R compiled_strings.zip --auto-add-overlay
python -c "import zipfile; zipfile.ZipFile('manifest_fixed.zip','r').extract('AndroidManifest.xml','origin_temp')"
copy /y "origin_temp\AndroidManifest.xml" "origin\AndroidManifest.xml"
rmdir /s /q origin_temp 2>nul
del manifest_fixed.zip compiled_strings.zip 2>nul

echo === [6/6] Packaging APK ===
python package_apk.py
"%BT%\zipalign.exe" -f 4 installed_raw.apk installed_aligned.apk
"%BT%\apksigner.bat" sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out installed.apk installed_aligned.apk
del installed_raw.apk installed_aligned.apk 2>nul

echo === DONE: installed.apk ===
goto :end

:error
echo === BUILD FAILED ===
exit /b 1

:end
endlocal
