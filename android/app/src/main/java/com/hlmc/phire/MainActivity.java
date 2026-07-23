package com.hlmc.phire;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
// nanoTime maps to CLOCK_MONOTONIC, matching Rust's get_uptime()
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
import quad_native.QuadNative;

public class MainActivity extends Activity {
    private static final int REQUEST_FILE_PICK = 1001;
    private static final int REQUEST_FILE_PICK_RESPACK = 1002;
    private QuadSurface view;

    static {
        System.loadLibrary("phire_ui");
    }

    public static class QuadSurface extends SurfaceView implements View.OnTouchListener, View.OnKeyListener, SurfaceHolder.Callback {
        public QuadSurface(Context context) {
            super(context);
            getHolder().addCallback(this);
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
            setOnTouchListener(this);
            setOnKeyListener(this);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            Log.i("SAPP", "surfaceCreated");
            QuadNative.surfaceOnSurfaceCreated(holder.getSurface());
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i("SAPP", "surfaceDestroyed");
            QuadNative.surfaceOnSurfaceDestroyed(holder.getSurface());
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i("SAPP", "surfaceChanged");
            QuadNative.surfaceOnSurfaceChanged(holder.getSurface(), width, height);
        }

        public boolean onTouch(View v, MotionEvent event) {
            if (event == null) return true;
            int pointerCount = event.getPointerCount();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    for (int i = 0; i < pointerCount; i++) {
                        QuadNative.surfaceOnTouch(event.getPointerId(i), 0, event.getX(i), event.getY(i), System.nanoTime() / 1_000_000);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    QuadNative.surfaceOnTouch(event.getPointerId(0), 1, event.getX(0), event.getY(0), System.nanoTime() / 1_000_000);
                    break;
                case MotionEvent.ACTION_DOWN:
                    QuadNative.surfaceOnTouch(event.getPointerId(0), 2, event.getX(0), event.getY(0), System.nanoTime() / 1_000_000);
                    break;
                case MotionEvent.ACTION_POINTER_UP: {
                    int idx = event.getActionIndex();
                    QuadNative.surfaceOnTouch(event.getPointerId(idx), 1, event.getX(idx), event.getY(idx), System.nanoTime() / 1_000_000);
                    break;
                }
                case MotionEvent.ACTION_POINTER_DOWN: {
                    int idx = event.getActionIndex();
                    QuadNative.surfaceOnTouch(event.getPointerId(idx), 2, event.getX(idx), event.getY(idx), System.nanoTime() / 1_000_000);
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                    for (int i = 0; i < pointerCount; i++) {
                        QuadNative.surfaceOnTouch(event.getPointerId(i), 3, event.getX(i), event.getY(i), System.nanoTime() / 1_000_000);
                    }
                    break;
            }
            return true;
        }

        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event == null) return true;
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode != 0) {
                QuadNative.surfaceOnKeyDown(keyCode);
            }
            if (event.getAction() == KeyEvent.ACTION_UP && keyCode != 0) {
                QuadNative.surfaceOnKeyUp(keyCode);
            }
            if (event.getAction() == KeyEvent.ACTION_UP || event.getAction() == KeyEvent.ACTION_MULTIPLE) {
                int character = event.getUnicodeChar();
                if (character == 0) {
                    CharSequence characters = event.getCharacters();
                    if (characters != null && characters.length() > 0) {
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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setDecorFitsSystemWindows(false);

        // Immersive mode: hide system bars and prevent gesture navigation
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(flags);

        view = new QuadSurface(this);
        setContentView(view);

        java.io.File dataDir = getExternalFilesDir(null);
        if (dataDir == null) dataDir = getFilesDir();
        QuadNative.setDataPath(dataDir.getAbsolutePath());
        QuadNative.setTempDir(getCacheDir().getAbsolutePath());
        QuadNative.setDpi((int)(getResources().getDisplayMetrics().density * 160));
        QuadNative.initializeContext(this);
        QuadNative.activityOnCreate(this);
    }

    public void onResume() {
        super.onResume();
        QuadNative.activityOnResume();
        QuadNative.libActivityOnResume();
    }

    public void onPause() {
        super.onPause();
        QuadNative.activityOnPause();
        QuadNative.libActivityOnPause();
    }

    public void onDestroy() {
        super.onDestroy();
        QuadNative.activityOnDestroy();
        QuadNative.libActivityOnDestroy();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        QuadNative.libActivityOnWindowFocusChanged(hasFocus);
    }

    public void onBackPressed() {}

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri == null) return;
            String path = getPathFromUri(uri);
            if (path == null) return;
            if (requestCode == REQUEST_FILE_PICK_RESPACK) {
                QuadNative.markImportRespack();
            }
            QuadNative.setChosenFile(path);
        }
    }

    private String getPathFromUri(android.net.Uri uri) {
        try {
            String path = uri.getPath();
            if (path != null && path.contains(":")) {
                String[] parts = path.split(":");
                if (parts.length > 1) path = parts[1];
            }
            if (path != null && !path.startsWith("/")) {
                path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + path;
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }

    public void setFullScreen(final boolean fullscreen) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (fullscreen) {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                    getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    getWindow().setDecorFitsSystemWindows(false);
                } else {
                    getWindow().setDecorFitsSystemWindows(true);
                }
            }
        });
    }

    public void showKeyboard(final boolean show) {
        runOnUiThread(new Runnable() {
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) return;
                if (show) {
                    imm.showSoftInput(view, 0);
                } else if (view != null && view.getWindowToken() != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });
    }

    public void openUrl(final String url) {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
                } catch (Exception e) {
                    Log.e("PhiWOW", "Failed to open URL: " + url, e);
                }
            }
        });
    }

    public void inputText(final String text, final boolean isPassword, final String title, final String hint) {
        runOnUiThread(new Runnable() {
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
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        QuadNative.setInputText(input.getText().toString());
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        QuadNative.setInputText("");
                    }
                });
                builder.show();
            }
        });
    }

    public void chooseFile() {
        runOnUiThread(new Runnable() {
            public void run() {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_FILE_PICK);
            }
        });
    }
}
