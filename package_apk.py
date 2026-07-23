import zipfile, os
with zipfile.ZipFile('installed_raw.apk', 'w') as zf:
    for root, dirs, files in os.walk('origin'):
        for f in sorted(files):
            full = os.path.join(root, f)
            arcname = os.path.relpath(full, 'origin').replace(os.sep, '/')
            zf.write(full, arcname, compress_type=zipfile.ZIP_STORED)
print('APK created:', os.path.getsize('installed_raw.apk'), 'bytes')
