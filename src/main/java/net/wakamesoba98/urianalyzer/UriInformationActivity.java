package net.wakamesoba98.urianalyzer;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import net.wakamesoba98.urianalyzer.databinding.UriInformationActivityBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UriInformationActivity extends AppCompatActivity {

    private List<Map<String, String>> parentList = new ArrayList<>();
    private List<List<Map<String, String>>> childList = new ArrayList<>();
    private Uri fileUri, contentUri;
    private String mimeType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UriInformationActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.uri_information_activity);

        SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                this,
                parentList, android.R.layout.simple_expandable_list_item_1, new String[]{"main"}, new int[]{android.R.id.text1},
                childList, android.R.layout.simple_expandable_list_item_2, new String[]{"main", "sub"}, new int[]{android.R.id.text1, android.R.id.text2});
        binding.expandableListView.setAdapter(adapter);
        binding.expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Toast.makeText(UriInformationActivity.this, childList.get(groupPosition).get(childPosition).get("sub"), Toast.LENGTH_LONG).show();
                return false;
            }
        });

        Intent intent = getIntent();
        if (intent != null){
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                generateUriInformationItems(uri);
                generateUriUtilsItems(uri);
                generateContentProviderItems(uri);
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuShareFileUri:
                onShareFileUriClick();
                break;

            case R.id.menuShareContentUri:
                onShareContentUriClick();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onShareFileUriClick() {
        if (fileUri != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }

    public void onShareContentUriClick() {
        if (contentUri != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }

    private void generateUriInformationItems(Uri uri) {
        mimeType = getContentResolver().getType(uri);
        List<Map<String, String>> itemList = new ArrayList<>();
        generateChildItem(itemList, "URI", uri.toString());
        generateChildItem(itemList, "Scheme", uri.getScheme());
        generateChildItem(itemList, "Authority", uri.getAuthority());
        generateChildItem(itemList, "Path", uri.getPath());
        generateChildItem(itemList, "MIMEType", mimeType);
        generateParentItem("URI Information", itemList);
    }

    private void generateUriUtilsItems(Uri uri) {
        UriUtils utils = new UriUtils();
        String filePath = utils.getFilePath(this, uri);
        List<Map<String, String>> itemList = new ArrayList<>();
        generateChildItem(itemList, "getFilePath", filePath);
        generateChildItem(itemList, "getFileName", utils.getFileName(this, uri));
        generateChildItem(itemList, "getFileSize", String.valueOf(utils.getFileSize(this, uri)));
        generateChildItem(itemList, "getOrientationFromMediaStore", String.valueOf(utils.getOrientationFromMediaStore(this, uri)));
        generateChildItem(itemList, "getOrientationFromExif", String.valueOf(utils.getOrientationFromExif(this, uri)));
        if (filePath != null) {
            fileUri = utils.uriToFileToUri(this, uri);
            generateChildItem(itemList, "Uri -> File -> Uri.fromFile", fileUri.toString());
        }
        contentUri = utils.uriToFileToUri24(this, uri);
        if (contentUri != null) {
            generateChildItem(itemList, "Uri -> File -> FileProvider.getUriForFile", contentUri.toString());
        }
        generateParentItem("UriUtils", itemList);
    }

    private void generateContentProviderItems(Uri uri) {
        UriUtils utils = new UriUtils();
        List<Map<String, String>> itemList = new ArrayList<>();
        Map<String, String> map = utils.getAllInformation(this, uri);
        for (String key : map.keySet()) {
            generateChildItem(itemList, key, map.get(key));
        }
        generateParentItem("ContentProvider", itemList);
    }

    private void generateChildItem(List<Map<String, String>> itemList, String main, String sub) {
        Map<String, String> map = new HashMap<>();
        map.put("main", main);
        map.put("sub", sub);
        itemList.add(map);
    }

    private void generateParentItem(String main, List<Map<String, String>> child) {
        childList.add(child);
        Map<String, String> parentMap = new HashMap<>();
        parentMap.put("main", main);
        parentList.add(parentMap);
    }
}
