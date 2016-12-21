package com.compeovario;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.compeovario.filepicker.FilePicker;
import com.compeovario.filepicker.FilterByFileExtension;
import com.compeovario.util.CreateThermicFileAsync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;

public class MenuActivity extends Activity {

    private Button btnmenusettings,btnmenutask,btnmenufiles;

    private static final String TAG = "MenuActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menulayout);


        btnmenusettings = (Button) findViewById(R.id.btnmenusettings);
        btnmenutask = (Button) findViewById(R.id.btnmenutask);
        btnmenufiles = (Button) findViewById(R.id.btnmenufiles);

        btnmenusettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(getApplicationContext(), SettingsActivity.class),
                        OTPApp.SETTINGS_REQUEST_CODE);
            }
        });

        btnmenutask.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(
                new Intent(getApplicationContext(), TaskEditor.class),
                        OTPApp.TASK_REQUEST_CODE);
            }
        });

        btnmenufiles.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(getApplicationContext(), FilesActivity.class),
                        OTPApp.FILES_REQUEST_CODE);
            }
        });
    }
}


