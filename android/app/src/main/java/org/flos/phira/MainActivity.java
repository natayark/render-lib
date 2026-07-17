package org.flos.phira;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.io.File;

import quad_native.QuadNative;

class QuadSurface
    extends
        SurfaceView
    implements
        View.OnTouchListener,
        View.OnKeyListener,
        SurfaceHolder.Callback {

    public QuadSurface(Context context) {
        super(context);
        getHolder().addCallback(this);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        setOnTouchListener(this);
        setOnKeyListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("SAPP", "surfaceCreated");
        Surface surface = holder.getSurface();
        QuadNative.surfaceOnSurfaceCreated(surface);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("SAPP", "surfaceDestroyed");
        Surface surface = holder.getSurface();
        QuadNative.surfaceOnSurfaceDestroyed(surface);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("SAPP", "surfaceChanged");
        Surface surface = holder.getSurface();
        QuadNative.surfaceOnSurfaceChanged(surface, width, height);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int pointerCount = event.getPointerCount();
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < pointerCount; i++) {
                    final int id = event.getPointerId(i);
                    final float x = event.getX(i);
                    final float y = event.getY(i);
                    QuadNative.surfaceOnTouch(id, 0, x, y);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                final int id = event.getPointerId(0);
                final float x = event.getX(0);
                final float y = event.getY(0);
                QuadNative.surfaceOnTouch(id, 1, x, y);
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                final int id = event.getPointerId(0);
                final float x = event.getX(0);
                final float y = event.getY(0);
                QuadNative.surfaceOnTouch(id, 2, x, y);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = event.getActionIndex();
                final int id = event.getPointerId(pointerIndex);
                final float x = event.getX(pointerIndex);
                final float y = event.getY(pointerIndex);
                QuadNative.surfaceOnTouch(id, 1, x, y);
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int pointerIndex = event.getActionIndex();
                final int id = event.getPointerId(pointerIndex);
                final float x = event.getX(pointerIndex);
                final float y = event.getY(pointerIndex);
                QuadNative.surfaceOnTouch(id, 2, x, y);
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                for (int i = 0; i < pointerCount; i++) {
                    final int id = event.getPointerId(i);
                    final float x = event.getX(i);
                    final float y = event.getY(i);
                    QuadNative.surfaceOnTouch(id, 3, x, y);
                }
                break;
            }
            default:
                break;
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode != 0) {
            QuadNative.surfaceOnKeyDown(keyCode);
        }

        if (event.getAction() == KeyEvent.ACTION_UP && keyCode != 0) {
            QuadNative.surfaceOnKeyUp(keyCode);
        }

        if (event.getAction() == KeyEvent.ACTION_UP || event.getAction() == KeyEvent.ACTION_MULTIPLE) {
            int character = event.getUnicodeChar();
            if (character == 0) {
                String characters = event.getCharacters();
                if (characters != null && characters.length() >= 0) {
                    character = characters.charAt(0);
                }
            }

            if (character != 0) {
                QuadNative.surfaceOnCharacter(character);
            }
        }

        return true;
    }

    public Surface getNativeSurface() {
        return getHolder().getSurface();
    }
}

public class MainActivity extends Activity {

    private static final int REQUEST_FILE_PICK = 1001;
    private static final int REQUEST_FILE_PICK_RESPACK = 1002;
    private QuadSurface view;
    private boolean importRespack = false;

    static {
        System.loadLibrary("phire_ui");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        getWindow().getAttributes().layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setDecorFitsSystemWindows(false);

        view = new QuadSurface(this);
        setContentView(view);

        // Set data path
        File dataDir = getExternalFilesDir(null);
        if (dataDir == null) {
            dataDir = getFilesDir();
        }
        QuadNative.setDataPath(dataDir.getAbsolutePath());

        // Set temp dir
        File cacheDir = getCacheDir();
        QuadNative.setTempDir(cacheDir.getAbsolutePath());

        // Set DPI
        int dpi = (int) (getResources().getDisplayMetrics().density * 160);
        QuadNative.setDpi(dpi);

        QuadNative.activityOnCreate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        QuadNative.activityOnResume();
        QuadNative.libActivityOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        QuadNative.activityOnPause();
        QuadNative.libActivityOnPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        QuadNative.activityOnDestroy();
        QuadNative.libActivityOnDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        QuadNative.libActivityOnWindowFocusChanged(hasFocus);
    }

    @Override
    public void onBackPressed() {
        // Let Rust handle back press
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri != null) {
                String path = getPathFromUri(uri);
                if (path != null) {
                    if (requestCode == REQUEST_FILE_PICK_RESPACK) {
                        QuadNative.markImportRespack();
                    }
                    QuadNative.setChosenFile(path);
                }
            }
        }
    }

    private String getPathFromUri(android.net.Uri uri) {
        try {
            String path = uri.getPath();
            if (path != null && path.contains(":")) {
                String[] parts = path.split(":");
                if (parts.length > 1) {
                    path = parts[1];
                }
            }
            if (path != null && !path.startsWith("/")) {
                path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + path;
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }

    // Called from Rust via JNI
    public void setFullScreen(final boolean fullscreen) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (fullscreen) {
                    getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    );
                    getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    getWindow().setDecorFitsSystemWindows(false);
                } else {
                    getWindow().setDecorFitsSystemWindows(true);
                }
            }
        });
    }

    // Called from Rust via JNI
    public void showKeyboard(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) return;
                if (show) {
                    imm.showSoftInput(view, 0);
                } else {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });
    }

    // Called from Rust via JNI
    public void openUrl(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("PhiWOW", "Failed to open URL: " + url, e);
                }
            }
        });
    }

    // Called from Rust via JNI
    public void inputText(final String text, final boolean isPassword, final String title, final String hint) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(title != null ? title : "");

                final EditText input = new EditText(MainActivity.this);
                input.setText(text != null ? text : "");
                input.setHint(hint != null ? hint : "");
                if (isPassword) {
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                builder.setView(input);

                builder.setPositiveButton("OK", (dialog, which) -> {
                    String result = input.getText().toString();
                    QuadNative.setInputText(result);
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> {
                    QuadNative.setInputText("");
                });

                builder.show();
            }
        });
    }

    // Called from Rust via JNI
    public void chooseFile() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_FILE_PICK);
            }
        });
    }
}
