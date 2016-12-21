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

public class FilesActivity extends Activity {

    private Button btnopentask,btnopenwp,btndeletewp,btndeletethemals,btndeletask,btncreatethermics;
    private TextView txt_filestatus,txt_filespath;
    private static final String TAG = "FilesActivity";

    private final static int WPFILE = 0;
    private final static int TASKFILE = 1;
    private final static int IGC = 2;

    String taskFileName = "Default.tsk";
    String thermicFileName = "Thermics.txt";
    String wpFileName = "Default.cup";
    String startString = "<Point type=";
    String endString = "</Point>";
    WPManager wpmanager = null;
    InputStream in_s = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fileslayout);

        txt_filestatus = (TextView)findViewById(R.id.txt_filestatus);
        txt_filespath = (TextView)findViewById(R.id.txt_filespath);
        btnopentask = (Button) findViewById(R.id.btnopentask);
        btnopenwp = (Button) findViewById(R.id.btnopenwp);
        btndeletewp = (Button) findViewById(R.id.btndeletewp);
        btndeletethemals = (Button) findViewById(R.id.btndeletethemals);
        btndeletask = (Button) findViewById(R.id.btndeletask);
        btncreatethermics = (Button) findViewById(R.id.btncreatethermics);

        wpmanager = new WPManager(this);

        btnopentask.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startTaskFilePicker();
            }
        });

        btnopenwp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startWpFilePicker();
            }
        });

        btndeletewp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deleteWpFile();
            }
        });

        btndeletethemals.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deleteThermicFile();
            }
        });

        btndeletask.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deleteTaskFile();
            }
        });

        btncreatethermics.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startIgcFilePicker();
            }
        });

        updateFileTxt();

    }

    public void setTxt_filestatus(String str) {
        this.txt_filestatus.setText(str);
        updateFileTxt();
    }

    private void updateFileTxt() {

        String empty = "\t";
        String taskstr = "No File";
        String wpstr = "No File";
        String thermicstr = "No File";

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, taskFileName);

        if (myFile.exists())
        {
            taskstr = taskFileName;
        }

        myFile = new File(root, wpFileName);

        if (myFile.exists())
        {
            wpstr = wpFileName;
        }

        myFile = new File(root, thermicFileName);

        if (myFile.exists())
        {
            thermicstr = thermicFileName;
        }

        txt_filespath.setText("\nFiles in CompeoVario\n"
                + "Task" + empty + empty + empty + empty + empty + empty + empty + empty + ":" + empty + empty + taskstr
                + empty + empty + "   Total Points : " + String.valueOf(getTaskPoints()) +  "\n"
                + "Waypoints" + empty + empty + ":" + empty + empty + wpstr
                + empty + empty + "   Total Points : " + String.valueOf(getWpPoints()) +  "\n"
                + "Thermics" + empty + empty + empty + ":" + empty + empty + thermicstr
                + empty + "   Total Points : " + String.valueOf(getThermicPoints())
        );
    }

    private void startWpFilePicker() {
        FilePicker.setFileDisplayFilter(new FilterByFileExtension(".cup;.CUP"));
        startActivityForResult(new Intent(this, FilePicker.class), WPFILE);
    }

    private void startTaskFilePicker() {
        FilePicker.setFileDisplayFilter(new FilterByFileExtension(".tsk"));
        startActivityForResult(new Intent(this, FilePicker.class), TASKFILE);
    }

    private void startIgcFilePicker() {
        FilePicker.setFileDisplayFilter(new FilterByFileExtension(".igc;.IGC"));
        startActivityForResult(new Intent(this, FilePicker.class), IGC);
    }

    public void deleteWpFile() {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, wpFileName);

        if (myFile.exists()) {
            myFile.delete();
            Log.d(TAG, "Log File deleted: " + myFile.getName());
            txt_filestatus.setText("Wp File deleted: " + myFile.getName());
            updateFileTxt();
        }
    }

    public void deleteThermicFile() {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, thermicFileName);

        if (myFile.exists()) {
            myFile.delete();
            Log.d(TAG, "Log File deleted: " + myFile.getName());
            txt_filestatus.setText("Thermic File deleted: " + myFile.getName());
            updateFileTxt();
        }
    }

    public void deleteTaskFile() {

        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
        File myFile = new File(root, taskFileName);

        if (myFile.exists()) {
            myFile.delete();
            Log.d(TAG, "Log File deleted: " + myFile.getName());
            txt_filestatus.setText("Task File deleted: " + myFile.getName());
            updateFileTxt();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case WPFILE:
                if (resultCode == this.RESULT_OK) {
                    if (data != null && data.getStringExtra(FilePicker.SELECTED_FILE) != null) {
                        String filePath = data.getStringExtra(FilePicker.SELECTED_FILE);
                        try {
                            if (filePath.contains(".cup")) {
                                copyWpFile(filePath);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                break;
            case TASKFILE:
                if (resultCode == MyActivity.RESULT_OK) {
                    if (data != null && data.getStringExtra(FilePicker.SELECTED_FILE) != null) {
                        String filePath = data.getStringExtra(FilePicker.SELECTED_FILE);
                        try {
                            if (filePath.contains(".tsk")) {
                                copyTaskFile(filePath);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                break;
            case IGC:
                if (resultCode == MyActivity.RESULT_OK) {
                    if (data != null && data.getStringExtra(FilePicker.SELECTED_FILE) != null) {
                        String filePath = data.getStringExtra(FilePicker.SELECTED_FILE);
                        createThermicsFromIgc(filePath);
                    }
                }
                break;
        }
    }

    public void createThermicsFromIgc(String sourceFilePath) {
        new CreateThermicFileAsync(this).execute(sourceFilePath);
    }

    public void copyWpFile(String sourceFilePath) {

        try {

            if (sourceFilePath.contains("Default.cup")) {
                return;
            }

            FileChannel source = null;
            FileChannel destination = null;

            File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
            File destFile = new File(root, wpFileName);
            File sourceFile = new File(sourceFilePath);

            try {
                source = new FileInputStream(sourceFile).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
                Log.d(TAG, "LogData Wp File copied: " + sourceFile.getName());
                txt_filestatus.setText("Wp File copied: " + sourceFile.getName());

            } finally {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            }

        } catch (Exception e) {
        }

        updateFileTxt();
    }

    public void copyTaskFile(String sourceFilePath) {

        try {

            FileChannel source = null;
            FileChannel destination = null;

            File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");
            File destFile = new File(root, taskFileName);
            File sourceFile = new File(sourceFilePath);

            if(sourceFile.getAbsolutePath().contains("CompeoVario/Default.tsk"))
            {
                return;
            }

            try {
                source = new FileInputStream(sourceFile).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
                Log.d(TAG, "Log Task File copied: " + sourceFile.getName());
                txt_filestatus.setText("Task File copied: " + sourceFile.getName());

            } finally {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            }

        } catch (Exception e) {
        }

        updateFileTxt();
    }

    public int getTaskPoints() {
        int index = 0;
        try {
            File myFile = null;
            File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");

            myFile = new File(root, taskFileName);
            boolean findWp = false;

            if (myFile.exists()) {
                in_s = new FileInputStream(myFile);
                try {
                    String str = "";
                    StringBuffer buf = new StringBuffer();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in_s));
                    while ((str = reader.readLine()) != null) {

                        if (str.indexOf(startString) != -1) {
                            findWp = true;
                        }
                        if (findWp) {
                            buf.append(str);
                            if (buf.indexOf(endString) != -1) {
                                index++;
                                buf.delete(0, buf.length());
                            }
                        }
                    }

                } finally {
                    try {
                        in_s.close();
                    } catch (Throwable ignore) {
                    }
                }
            }

        } catch (Exception e) {
        }
        return index;
    }

    public int getWpPoints() {
        int index = 0;
        try {
            File myFile = null;
            File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");

            myFile = new File(root,wpFileName);
            if (myFile.exists()) {
                in_s = new FileInputStream(myFile);
                try {
                    String str = "";
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in_s));
                    while ((str = reader.readLine()) != null) {
                        index++;
                    }

                } finally {
                    try {
                        in_s.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Exception e) {
        }
        return index;
    }

    public int getThermicPoints() {
        int index = 0;
        try {
            File myFile = null;
            File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");

            myFile = new File(root,thermicFileName);
            if (myFile.exists()) {
                in_s = new FileInputStream(myFile);
                try {
                    String str = "";
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in_s));
                    while ((str = reader.readLine()) != null) {
                        index++;
                    }

                } finally {
                    try {
                        in_s.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Exception e) {
        }
        return index;
    }

    public void createThermicsFromIGC(String sourceFilePath) {

        try {
            File sourceFile = new File(sourceFilePath);

            try {


            } finally {

            }

        } catch (Exception e) {
        }

        updateFileTxt();
    }
}


