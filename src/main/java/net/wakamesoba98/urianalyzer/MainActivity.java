package net.wakamesoba98.urianalyzer;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import net.wakamesoba98.urianalyzer.databinding.MainActivityBinding;

public class MainActivity extends AppCompatActivity implements MainActivityHandler {

    private static final int REQUEST_PICK = 1;
    private static final int REQUEST_PERMISSION = 2;

    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.main_activity);
        binding.setHandler(this);
    }

    @Override
    public void onActionPickClick(View view) {
        Intent intentPick = new Intent(Intent.ACTION_PICK);
        intentPick.setType("image/*");
        startActivityForResult(intentPick, REQUEST_PICK);
    }

    @Override
    public void onActionGetContentClick(View view) {
        Intent intentPick = new Intent(Intent.ACTION_GET_CONTENT);
        intentPick.setType("image/*");
        startActivityForResult(intentPick, REQUEST_PICK);
    }

    @Override
    public void onActionOpenDocumentClick(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent intentPick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intentPick.setType("image/*");
            startActivityForResult(intentPick, REQUEST_PICK);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK && resultCode == RESULT_OK) {
            uri = data.getData();
            if (uri == null) {
                return;
            }
            if (UriUtils.AUTHORITY_MEDIA.equals(uri.getAuthority())) {
                if (PermissionUtil.checkSelfPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showUriInformation(uri);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
                    }
                }
            } else {
                showUriInformation(uri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0) {
            if (PermissionUtil.verifyPermissions(grantResults)) {
                showUriInformation(uri);
            } else {
                Toast.makeText(this, R.string.failed_to_grant_privilege, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showUriInformation(Uri uri) {
        Intent intent = new Intent(this, UriInformationActivity.class);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(intent);
    }
}
