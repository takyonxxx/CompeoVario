package com.compeovario;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.compeovario.filepicker.FilePicker;
import com.compeovario.filepicker.FilterByFileExtension;
import com.compeovario.util.ItemAdapter;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.woxthebox.draglistview.DragListView;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by tbiliyor on 29.11.2016.
 */

public class TaskEditor extends Activity {
    private final static int TASKFILE = 0;
    static EditText edit_starttime, edit_endtime;
    static String starttime = null;
    static String endtime = null;
    static ArrayList<HashMap<String, String>> task = new ArrayList<HashMap<String, String>>();
    String taskFileName = "Default.tsk";
    String wpFileName = "Default.cup";

    public ArrayList<TP> turnpoints = new ArrayList<TP>();
    public ArrayList<WP> wppoints = new ArrayList<WP>();
    SimpleAdapter adaptertask;
    private ArrayList<Pair<Long, String>> mItemArray;
    TaskManager taskmanager = null;
    WPManager wpmanager = null;
    View textEntryView;
    Spinner task_pointtype, task_pointshape;
    EditText task_pointradius;
    private ListView listwp;
    private DragListView mDragListView;
    private Button btn_openTask, btn_dellTask, btn_addWp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.task_list_view);

        btn_openTask = (Button) findViewById(R.id.btn_openTask);
        btn_dellTask = (Button) findViewById(R.id.btn_dellTask);
        btn_addWp = (Button) findViewById(R.id.btn_addWp);

        btn_openTask.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                startTaskFilePicker();
            }
        });

        btn_addWp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if(wppoints.size() == 0) {
                    startWpFilePicker();
                }
                else
                {
                    openWpDialog();
                }
            }
        });

        btn_dellTask.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                taskmanager.deleteTaskFile();
                gettask();
            }
        });

        edit_starttime = (EditText) findViewById(R.id.txt_starttime);
        edit_endtime = (EditText) findViewById(R.id.txt_endtime);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                edit_starttime.requestFocus();
            }
        }, 100);

        taskmanager = new TaskManager(this, null);
        taskmanager.createTask();

        Date date = new Date(taskmanager.getTask_Starttime());
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        starttime = formatter.format(date);

        date = new Date(taskmanager.getTask_Endtime());
        formatter = new SimpleDateFormat("HH:mm");
        endtime = formatter.format(date);

        edit_starttime.setText(starttime);
        edit_endtime.setText(endtime);

        wpmanager = new WPManager(this);

        edit_starttime.setInputType(InputType.TYPE_NULL);
        edit_starttime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //showTruitonTimePickerDialogS(v);
                    starttime = edit_starttime.getText().toString() + ":00";
                    //taskmanager.setTaskStartTime(starttime);

                    endtime = edit_endtime.getText().toString() + ":00";
                    //taskmanager.setTaskEndTime(endtime);
                }
            }
        });
        edit_starttime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTruitonTimePickerDialogS(v);
            }
        });

        edit_endtime.setInputType(InputType.TYPE_NULL);
        edit_endtime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //showTruitonTimePickerDialogE(v);
                }
            }
        });
        edit_endtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTruitonTimePickerDialogE(v);
            }
        });

        mDragListView = (DragListView) findViewById(R.id.drag_list_view);
        mDragListView.getRecyclerView().setVerticalScrollBarEnabled(true);
        mDragListView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragStarted(int position) {
                //Toast.makeText(mDragListView.getContext(), "Start - position: " + position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                if (fromPosition != toPosition) {
                    taskmanager.moveTaskPoint(fromPosition,toPosition);
                    gettask();
                    Toast.makeText(mDragListView.getContext(), "Moved from: " + fromPosition + " to " + toPosition, Toast.LENGTH_SHORT).show();
                }
            }
        });


        mItemArray = new ArrayList<>();

        gettask();

    }

    public void showTruitonTimePickerDialogS(View v) {
        DialogFragment newFragment = new TimePickerFragmentS();
        android.app.FragmentManager fm = getFragmentManager();
        newFragment.show(fm, "timePicker");
    }

    public void showTruitonTimePickerDialogE(View v) {
        DialogFragment newFragment = new TimePickerFragmentE();
        android.app.FragmentManager fm = getFragmentManager();
        newFragment.show(fm, "timePicker");
    }

    public void gettask() {

        double taskdistance = 0;
        mItemArray.clear();
        taskmanager.createTask();

        File myFile = null;
        File root = new File(Environment.getExternalStorageDirectory(), "CompeoVario");

        myFile = new File(root, wpFileName);

        if (myFile.exists()) {
            wppoints = wpmanager.getWPFromFile(myFile.getAbsolutePath());
        }

        if (taskmanager.isTaskCreated()) {

            LatLng from = taskmanager.getStartLatLng();
            LatLng to = null;

            turnpoints = taskmanager.getTaskEdgePoints();
            if(turnpoints.size() > 0)
            {

                DecimalFormat df = new DecimalFormat("#.#");

                for (int i = 0; i < turnpoints.size(); i++) {

                    if (from == null) {
                        from = new LatLng(turnpoints.get(i).latitude, turnpoints.get(i).longitude);
                    }

                    to = new LatLng(turnpoints.get(i).latitude, turnpoints.get(i).longitude);

                    double distance = SphericalUtil.computeDistanceBetween(from, to);
                    taskdistance = taskdistance + distance;
                    Pair<Long, String> pair;

                    try {
                        if(!turnpoints.get(i).type.contains("Finish"))
                        {
                            pair = new Pair<Long, String>(
                                    Long.valueOf(i),
                                    turnpoints.get(i).name +
                                            ";" + turnpoints.get(i).type +
                                            ";" + turnpoints.get(i).shape +
                                            ";" + turnpoints.get(i).radius +
                                            ";" + df.format(taskdistance/1000));
                        }
                        else
                        {
                            pair = new Pair<Long, String>(
                                    Long.valueOf(i),
                                    turnpoints.get(i).name +
                                            ";" + turnpoints.get(i).type +
                                            ";" + turnpoints.get(i).shape +
                                            ";" + turnpoints.get(i).lenght +
                                            ";" + df.format(taskdistance/1000));
                        }

                        mItemArray.add(pair);

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    from = to;
                }
            }
        }

        mDragListView.setLayoutManager(new LinearLayoutManager(TaskEditor.this));
        ItemAdapter listAdapter = new ItemAdapter(this, mItemArray, R.layout.simple_row_view_task, R.id.move_image, true);
        mDragListView.setAdapter(listAdapter, true);
        mDragListView.setCanDragHorizontally(false);
    }

    private void startTaskFilePicker() {
        FilePicker.setFileDisplayFilter(new FilterByFileExtension(".tsk"));
        startActivityForResult(new Intent(this, FilePicker.class), TASKFILE);
    }

    private void startWpFilePicker() {
        FilePicker.setFileDisplayFilter(new FilterByFileExtension(".cup"));
        startActivityForResult(new Intent(this, FilePicker.class), TASKFILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TASKFILE:
                if (resultCode == MyActivity.RESULT_OK) {
                    if (data != null && data.getStringExtra(FilePicker.SELECTED_FILE) != null) {
                        String filePath = data.getStringExtra(FilePicker.SELECTED_FILE);
                        try {
                            if (filePath.contains(".cup")) {
                                wppoints = wpmanager.getWPFromFile(filePath);
                                openWpDialog();
                            } else if (filePath.contains(".tsk")) {
                                taskmanager.deleteTaskFile();
                                taskmanager.copyTaskFile(filePath);
                                taskmanager.createTask();
                                gettask();
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                break;
        }
    }

    private void openWpDialog() {

        LayoutInflater factory = LayoutInflater.from(TaskEditor.this);
        textEntryView = factory.inflate(R.layout.dialog_enter_wp, null);

        listwp = (ListView) textEntryView.findViewById(R.id.listwp);
        listwp.setClickable(true);

        task_pointradius = (EditText) textEntryView.findViewById(R.id.task_pointradius);
        task_pointtype = (Spinner) textEntryView.findViewById(R.id.task_pointtype);
        task_pointshape = (Spinner) textEntryView.findViewById(R.id.task_pointshape);


        task_pointradius.setText("400");

        int typselection = 1;
        task_pointtype.setSelection(typselection);

        typselection = 0;
        task_pointshape.setSelection(typselection);

        task.clear();

        for (int i = 0; i < wppoints.size(); i++) {

            try {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("name", wppoints.get(i).name);
                map.put("altitude", wppoints.get(i).altitude);

                task.add(map);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        adaptertask = new SimpleAdapter(TaskEditor.this,
                task, R.layout.simple_row_view_wp,
                new String[]{"name", "altitude"},
                new int[]{R.id.wp_name, R.id.wp_altitude});


        listwp.setAdapter(adaptertask);


        final AlertDialog.Builder alert = new AlertDialog.Builder(TaskEditor.this);
        //alert.setTitle("Touch to Add Turn Point...")
        alert.setView(textEntryView)

                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                            }
                        });

        final AlertDialog adTrueDialog;
        adTrueDialog = alert.show();

        listwp.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressWarnings("deprecation")
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                final int listindex = position;

                Object o = listwp.getItemAtPosition(position);

                String[] values = o.toString().split("name=");
                final String name = values[1].replace("name=", "").replace("}", "").replace("\"", "");

                for (int i = 0; i < wppoints.size(); i++) {
                    if (name.equals(wppoints.get(i).name)) {
                        taskmanager.addToTaskFile(
                                String.valueOf(wppoints.get(i).name),
                                String.valueOf(task_pointtype.getSelectedItem()),
                                String.valueOf(task_pointshape.getSelectedItem()),
                                Integer.parseInt(wppoints.get(i).altitude.replace(",", ".")),
                                Integer.parseInt(task_pointradius.getText().toString()),
                                Integer.parseInt(task_pointradius.getText().toString()),
                                Double.parseDouble(wppoints.get(i).latitude.replace(",", ".")),
                                Double.parseDouble(wppoints.get(i).longitude.replace(",", "."))
                        );
                        gettask();
                    }
                }

                adTrueDialog.cancel();
            }
        });
    }

    public static class TimePickerFragmentS extends DialogFragment implements
            TimePickerDialog.OnTimeSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
            edit_starttime.setText(String.format("%02d", (int) hourOfDay) + ":" + String.format("%02d", (int) minute));
            starttime = edit_starttime.getText().toString() + ":00";
            new TaskManager(getActivity(),null).setTaskStartTime(starttime);
        }
    }

    public static class TimePickerFragmentE extends DialogFragment implements
            TimePickerDialog.OnTimeSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
            edit_endtime.setText(String.format("%02d", (int) hourOfDay) + ":" + String.format("%02d", (int) minute) );
            endtime = edit_endtime.getText().toString() + ":00";
            new TaskManager(getActivity(),null).setTaskEndTime(endtime);
            //new TaskManager(getActivity(),null).updateTaskTimes(String.valueOf(taskmanager.getTask_Starttime()),String.valueOf(taskmanager.getTask_Endtime()));
        }
    }
}