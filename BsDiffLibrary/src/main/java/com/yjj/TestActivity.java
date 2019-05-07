package com.yjj;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.yjj.utils.DiffUtils;
import com.yjj.utils.R;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity {

    private Button patchContentInput;

    private static final String OLD_FILE_NAME = "oldfile.zip";
    private static final String NEW_FILE_NAME = "new_file";
    private static final String PATCH_FILE_NAME = "patch.patch";
    private static final String NEW_GENERATED_FILE_NAME = "patchnew.zip";

    private String generatedNewFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        patchContentInput = findViewById(R.id.patch_content);
        patchContentInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPatchButtonClicked();
            }
        });
    }

    private String getFilePath(String fileName) {
        return new File(getFilesDir(), fileName).getAbsolutePath();
    }

    public void onPatchButtonClicked() {
        final String oldFilePath = getFilePath(OLD_FILE_NAME);
        final String newFilePath = getFilePath(NEW_FILE_NAME);
        final String patchFilePath = getFilePath(PATCH_FILE_NAME);
        generatedNewFilePath = getFilePath(NEW_GENERATED_FILE_NAME);

        //String oldFileContent = oldContentInput.getText().toString();
        //String patchContent = patchContentInput.getText().toString();
        //
        //FileUtil.writeIntoFile(new File(oldFilePath), oldFileContent);
        //FileUtil.writeIntoFile(new File(newFilePath), oldFileContent + patchContent);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // DiffUtils.genDiff(oldFilePath, newFilePath, patchFilePath);
                DiffUtils.patch(oldFilePath, generatedNewFilePath, patchFilePath);
                updateNewContent();
            }
        }).start();
    }

    private void updateNewContent() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //newContentView.setText(FileUtil.readContentFromFile(new File(generatedNewFilePath)));
            }
        });
    }
}
