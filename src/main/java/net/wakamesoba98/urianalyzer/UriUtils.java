package net.wakamesoba98.urianalyzer;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.media.ExifInterface;
import android.support.v4.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

class UriUtils {

    static final String AUTHORITY_MEDIA = "com.android.providers.media.documents";
    private static final String AUTHORITY_DOWNLOADS = "com.android.providers.downloads.documents";
    private static final String SCHEME_CONTENT = "content";
    private static final String SCHEME_FILE = "file";

    Map<String, String> getAllInformation(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        Map<String, String> map = new LinkedHashMap<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    map.put(cursor.getColumnName(i), cursor.getString(i));
                }
            }
            cursor.close();
        }
        return map;
    }

    String getFilePath(Context context, Uri uri) {
        if (SCHEME_CONTENT.equals(uri.getScheme().toLowerCase())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (AUTHORITY_MEDIA.equals(uri.getAuthority())) {
                    return getPathFromDocumentsUri(context, uri);
                } else if (AUTHORITY_DOWNLOADS.equals(uri.getAuthority())) {
                    return getPathFromDownloadsUri(context, uri);
                }
            }
            String[] projection = {MediaStore.MediaColumns.DATA};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                String name = null;
                if (cursor.moveToFirst()) {
                    if (cursor.getColumnCount() > 0) {
                        name = cursor.getString(0);
                    }
                }
                cursor.close();
                return name;
            }
        } else if (SCHEME_FILE.equals(uri.getScheme().toLowerCase())) {
            return new File(uri.getPath()).getPath();
        }
        return null;
    }

    String getFileName(Context context, Uri uri) {
        if (SCHEME_CONTENT.equals(uri.getScheme().toLowerCase())) {
            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                String name = null;
                if (cursor.moveToFirst()) {
                    if (cursor.getColumnCount() > 0) {
                        name = cursor.getString(0);
                    }
                }
                cursor.close();
                return name;
            }
        } else if (SCHEME_FILE.equals(uri.getScheme().toLowerCase())) {
            return new File(uri.getPath()).getName();
        }
        return null;
    }

    long getFileSize(Context context, Uri uri) {
        if (SCHEME_CONTENT.equals(uri.getScheme().toLowerCase())) {
            String[] projection = {MediaStore.MediaColumns.SIZE};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                long size = -1;
                if (cursor.moveToFirst()) {
                    if (cursor.getColumnCount() > 0) {
                        size = cursor.getLong(0);
                    }
                }
                cursor.close();
                return size;
            }
        } else if (SCHEME_FILE.equals(uri.getScheme().toLowerCase())) {
            return new File(uri.getPath()).length();
        }
        return -1;
    }

    int getOrientationFromMediaStore(Context context, Uri uri) {
        String[] projection = {MediaStore.Images.ImageColumns.ORIENTATION};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int orientation = 0;
            if (cursor.moveToFirst()) {
                if (cursor.getColumnCount() > 0) {
                    orientation = cursor.getInt(0);
                }
            }
            cursor.close();
            return orientation;
        }
        return 0;
    }

    int getOrientationFromExif(Context context, Uri uri) {
        return getOrientationFromExif(context, uri, null);
    }

    int getOrientationFromExif(Context context, String path) {
        return getOrientationFromExif(context, null, path);
    }

    Uri uriToFileToUri(Context context, Uri uri) {
        return Uri.fromFile(new File(getFilePath(context, uri)));
    }

    Uri uriToFileToUri24(Context context, Uri uri) {
        InputStream is = null;
        OutputStream os = null;
        Uri sharableUri = null;
        try {
            File documentDir = new File(context.getFilesDir() + "/document");
            if (!documentDir.exists()) {
                documentDir.mkdir();
            }
            File shareFile = new File(documentDir + "/share_file");
            is = context.getContentResolver().openInputStream(uri);
            os = new FileOutputStream(shareFile);
            copy(is, os);
            sharableUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    shareFile
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (os != null) os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sharableUri;
    }

    private int getOrientationFromExif(Context context, Uri uri, String path) {
        BufferedInputStream bis = null;
        try {
            ExifInterface exif;
            if (uri != null) {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    return 0;
                }
                bis = new BufferedInputStream(inputStream);
                exif = new ExifInterface(bis);
            } else {
                exif = new ExifInterface(path);
            }
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;

                default:
                    return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private void copy(InputStream is, OutputStream os) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(is);
            bos = new BufferedOutputStream(os);
            byte[] buffer = new byte[8192];
            int numBytes;
            while ((numBytes = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, numBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(19)
    private String getPathFromDocumentsUri(Context context, Uri uri) {
        String id = DocumentsContract.getDocumentId(uri);
        String selection = "_id=?";
        String[] selectionArgs = new String[]{id.split(":")[1]};
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        if (cursor != null) {
            String path = null;
            if (cursor.moveToFirst()) {
                path = cursor.getString(0);
            }
            cursor.close();
            return path;
        }
        return null;
    }

    @RequiresApi(19)
    private String getPathFromDownloadsUri(Context context, Uri uri) {
        String id = DocumentsContract.getDocumentId(uri);
        Uri docUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = context.getContentResolver().query(docUri, projection, null, null, null);
        if (cursor != null) {
            String path = null;
            if (cursor.moveToFirst()) {
                path = cursor.getString(0);
            }
            cursor.close();
            return path;
        }
        return null;
    }
}
