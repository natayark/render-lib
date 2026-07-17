package com.hlmc.phire;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import quad_native.QuadNative;

public class ImportRespackActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                try {
                    String path = getPathFromUri(uri);
                    if (path != null) {
                        QuadNative.markImportRespack();
                        QuadNative.setChosenFile(path);
                    }
                } catch (Exception e) {
                    Log.e("PhiWOW", "Import respack failed", e);
                }
            }
        }

        finish();
    }

    private String getPathFromUri(Uri uri) {
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
}
