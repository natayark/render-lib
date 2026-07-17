package com.hlmc.phire

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import quad_native.QuadNative
import java.io.File

class QuadSurface(context: Context) : SurfaceView(context),
    View.OnTouchListener, View.OnKeyListener, SurfaceHolder.Callback {

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        setOnTouchListener(this)
        setOnKeyListener(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i("SAPP", "surfaceCreated")
        QuadNative.surfaceOnSurfaceCreated(holder.surface)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i("SAPP", "surfaceDestroyed")
        QuadNative.surfaceOnSurfaceDestroyed(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i("SAPP", "surfaceChanged")
        QuadNative.surfaceOnSurfaceChanged(holder.surface, width, height)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event ?: return true
        val pointerCount = event.pointerCount
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until pointerCount) {
                    QuadNative.surfaceOnTouch(event.getPointerId(i), 0, event.getX(i), event.getY(i))
                }
            }
            MotionEvent.ACTION_UP -> {
                QuadNative.surfaceOnTouch(event.getPointerId(0), 1, event.getX(0), event.getY(0))
            }
            MotionEvent.ACTION_DOWN -> {
                QuadNative.surfaceOnTouch(event.getPointerId(0), 2, event.getX(0), event.getY(0))
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                QuadNative.surfaceOnTouch(event.getPointerId(idx), 1, event.getX(idx), event.getY(idx))
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                QuadNative.surfaceOnTouch(event.getPointerId(idx), 2, event.getX(idx), event.getY(idx))
            }
            MotionEvent.ACTION_CANCEL -> {
                for (i in 0 until pointerCount) {
                    QuadNative.surfaceOnTouch(event.getPointerId(i), 3, event.getX(i), event.getY(i))
                }
            }
        }
        return true
    }

    @Suppress("DEPRECATION")
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        event ?: return true
        if (event.action == KeyEvent.ACTION_DOWN && keyCode != 0) {
            QuadNative.surfaceOnKeyDown(keyCode)
        }
        if (event.action == KeyEvent.ACTION_UP && keyCode != 0) {
            QuadNative.surfaceOnKeyUp(keyCode)
        }
        if (event.action == KeyEvent.ACTION_UP || event.action == KeyEvent.ACTION_MULTIPLE) {
            var character = event.unicodeChar
            if (character == 0) {
                val characters = event.characters
                if (!characters.isNullOrEmpty()) {
                    character = characters[0].code
                }
            }
            if (character != 0) {
                QuadNative.surfaceOnCharacter(character)
            }
        }
        return true
    }

    val nativeSurface: Surface get() = holder.surface
}

class MainActivity : Activity() {

    private var view: QuadSurface? = null

    companion object {
        init {
            System.loadLibrary("phire_ui")
        }

        private const val REQUEST_FILE_PICK = 1001
        private const val REQUEST_FILE_PICK_RESPACK = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.setDecorFitsSystemWindows(false)

        view = QuadSurface(this)
        setContentView(view)

        val dataDir = getExternalFilesDir(null) ?: filesDir
        QuadNative.setDataPath(dataDir.absolutePath)

        val cacheDir = cacheDir
        QuadNative.setTempDir(cacheDir.absolutePath)

        val dpi = (resources.displayMetrics.density * 160).toInt()
        QuadNative.setDpi(dpi)

        QuadNative.activityOnCreate(this)
    }

    override fun onResume() {
        super.onResume()
        QuadNative.activityOnResume()
        QuadNative.libActivityOnResume()
    }

    override fun onPause() {
        super.onPause()
        QuadNative.activityOnPause()
        QuadNative.libActivityOnPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        QuadNative.activityOnDestroy()
        QuadNative.libActivityOnDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        QuadNative.libActivityOnWindowFocusChanged(hasFocus)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            val path = getPathFromUri(uri) ?: return
            if (requestCode == REQUEST_FILE_PICK_RESPACK) {
                QuadNative.markImportRespack()
            }
            QuadNative.setChosenFile(path)
        }
    }

    private fun getPathFromUri(uri: android.net.Uri): String? {
        return try {
            var path = uri.path
            if (path != null && path.contains(":")) {
                val parts = path.split(":")
                if (parts.size > 1) path = parts[1]
            }
            if (path != null && !path.startsWith("/")) {
                path = Environment.getExternalStorageDirectory().absolutePath + "/" + path
            }
            path
        } catch (e: Exception) {
            null
        }
    }

    fun setFullScreen(fullscreen: Boolean) {
        runOnUiThread {
            if (fullscreen) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.setDecorFitsSystemWindows(false)
            } else {
                window.setDecorFitsSystemWindows(true)
            }
        }
    }

    fun showKeyboard(show: Boolean) {
        runOnUiThread {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return@runOnUiThread
            if (show) {
                imm.showSoftInput(view, 0)
            } else {
                view?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
            }
        }
    }

    fun openUrl(url: String) {
        runOnUiThread {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            } catch (e: Exception) {
                Log.e("PhiWOW", "Failed to open URL: $url", e)
            }
        }
    }

    fun inputText(text: String?, isPassword: Boolean, title: String?, hint: String?) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle(title ?: "")

            val input = EditText(this@MainActivity)
            input.setText(text ?: "")
            input.hint = hint ?: ""
            if (isPassword) {
                input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            builder.setView(input)

            builder.setPositiveButton("OK") { _, _ ->
                QuadNative.setInputText(input.text.toString())
            }
            builder.setNegativeButton("Cancel") { _, _ ->
                QuadNative.setInputText("")
            }
            builder.show()
        }
    }

    fun chooseFile() {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_FILE_PICK)
        }
    }
}
