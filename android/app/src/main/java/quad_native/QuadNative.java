package quad_native;

import android.view.Surface;

public class QuadNative {
    // miniquad framework - Activity lifecycle
    public native static void activityOnCreate(Object activity);
    public native static void activityOnResume();
    public native static void activityOnPause();
    public native static void activityOnDestroy();

    // miniquad framework - Surface lifecycle
    public native static void surfaceOnSurfaceCreated(Surface surface);
    public native static void surfaceOnSurfaceDestroyed(Surface surface);
    public native static void surfaceOnSurfaceChanged(Surface surface, int width, int height);

    // miniquad framework - Input
    public native static void surfaceOnTouch(int id, int phase, float x, float y);
    public native static void surfaceOnKeyDown(int keycode);
    public native static void surfaceOnKeyUp(int keycode);
    public native static void surfaceOnCharacter(int character);

    // PhiWOW / Phira extensions
    public native static void libActivityOnPause();
    public native static void libActivityOnResume();
    public native static void libActivityOnWindowFocusChanged(boolean hasFocus);
    public native static void libActivityOnDestroy();
    public native static void setDataPath(String path);
    public native static void setTempDir(String path);
    public native static void setDpi(int dpi);
    public native static void setChosenFile(String file);
    public native static void markImport();
    public native static void markImportRespack();
    public native static void setInputText(String text);
    public native static void antiAddictionCallback(int code);
    public native static void updateGyroScope(float x, float y, float z);
    public native static void updateGravity(float roll, float pitch, float yaw);
}
