
package com.compeovario.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.compeovario.R;
import com.compeovario.TaskEditor;
import com.compeovario.TaskManager;
import com.woxthebox.draglistview.DragItemAdapter;

import java.util.ArrayList;

public class ItemAdapter extends DragItemAdapter<Pair<Long, String>, ItemAdapter.ViewHolder> {

    TaskManager taskmanager = null;
    private int mLayoutId;
    private int mGrabHandleId;
    private boolean mDragOnLongPress;
    private Context context;
    View textEntryView;
    Spinner task_pointtype, task_pointshape;
    EditText task_pointradius;
    TextView task_pointname;

    public ItemAdapter(Context current, ArrayList<Pair<Long, String>> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        setHasStableIds(true);
        setItemList(list);
        this.context = current;
        taskmanager = new TaskManager(context, null);
        taskmanager.createTask();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        String text = mItemList.get(position).second;
        holder.tp_index.setText(String.valueOf(mItemList.get(position).first));
        holder.itemView.setTag(position);

        String[] values = text.toString().split(";");
        final String name = values[0];
        final String type = values[1];
        final String shape = values[2];
        final String radius = values[3];
        final String taskdistance = values[4];

        holder.tp_name.setText(name);
        holder.tp_type.setText(type);
        holder.tp_shape.setText(shape);
        holder.tp_radius.setText(radius);
        holder.tp_dist.setText(taskdistance);
    }

    @Override
    public long getItemId(int position) {
        return mItemList.get(position).first;
    }

    public void updateTaskPoint(final int position) {

        String taskPoint = taskmanager.getTaskPoint(position);
        String[] values = taskPoint.toString().split(";");
        final String name = values[0];
        final String type = values[1];
        final String shape = values[2];
        final String radius = values[3];
        final String lenght = values[4];
        Toast.makeText(context, "Item clicked " + taskPoint, Toast.LENGTH_SHORT).show();

        LayoutInflater factory = LayoutInflater.from(context);
        textEntryView = factory.inflate(R.layout.dialog_enter_task, null);

        task_pointname = (TextView) textEntryView.findViewById(R.id.task_pointname);
        task_pointradius = (EditText) textEntryView.findViewById(R.id.task_pointradius);
        task_pointtype = (Spinner) textEntryView.findViewById(R.id.task_pointtype);
        task_pointshape = (Spinner) textEntryView.findViewById(R.id.task_pointshape);

        task_pointname.setText(name);

        if(!type.contains("Finish"))
        {
            task_pointradius.setText(radius);
        }
        else
        {
            task_pointradius.setText(lenght);
        }


        int typselection = 0;
        if (type.equals("Start"))
            typselection = 0;
        else if (type.equals("Turn"))
            typselection = 1;
        else if (type.equals("Finish"))
            typselection = 2;

        task_pointtype.setSelection(typselection);

        typselection = 0;
        if (shape.equals("Cylinder"))
            typselection = 0;
        else if (shape.equals("Line"))
            typselection = 1;

        task_pointshape.setSelection(typselection);

        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setView(textEntryView)
                .setPositiveButton("Update",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                taskmanager.updateTaskFile(
                                        String.valueOf(task_pointname.getText()),
                                        String.valueOf(task_pointradius.getText()),
                                        String.valueOf(task_pointradius.getText()),
                                        String.valueOf(task_pointtype.getSelectedItem()),
                                        String.valueOf(task_pointshape.getSelectedItem()),
                                        position
                                );
                                ((TaskEditor)context).gettask();
                            }
                        })
                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        taskmanager.deleteTaskPoint(position);
                        ((TaskEditor)context).gettask();
                    }
                })

                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                            }
                        });
        alert.show();
    }

    public class ViewHolder extends DragItemAdapter.ViewHolder {

        public TextView tp_index, tp_name, tp_type, tp_shape, tp_radius, tp_dist;

        public ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);

            tp_index = (TextView) itemView.findViewById(R.id.tp_index);
            tp_name = (TextView) itemView.findViewById(R.id.tp_name);
            tp_type = (TextView) itemView.findViewById(R.id.tp_type);
            tp_shape = (TextView) itemView.findViewById(R.id.tp_shape);
            tp_radius = (TextView) itemView.findViewById(R.id.tp_radius);
            tp_dist = (TextView) itemView.findViewById(R.id.tp_dist);
        }

        @Override
        public void onItemClicked(View view) {
            Integer position = (Integer) view.getTag();
            updateTaskPoint(position);
        }

        @Override
        public boolean onItemLongClicked(View view) {
            Integer position = (Integer) view.getTag();
            return true;
        }
    }


}