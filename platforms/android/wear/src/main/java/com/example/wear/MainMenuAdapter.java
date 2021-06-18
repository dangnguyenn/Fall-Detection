package com.example.wear;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainMenuAdapter extends RecyclerView.Adapter<MainMenuAdapter.RecyclerViewHolder> {

    boolean flag = false;
    private ArrayList<MenuItem> dataSource = new ArrayList<MenuItem>();
    SwitchCompat reportSwitch;
    TextView statusText;

    public interface AdapterCallback {
        void onItemClicked(Integer menuPosition);
        void getSwitch(SwitchCompat reportSwitch);
        void getStatus(TextView statusText);
    }

    private AdapterCallback callback;
    private String drawableIcon;
    private Context context;


    public MainMenuAdapter(Context context, ArrayList<MenuItem> dataArgs, AdapterCallback callback) {
        this.context = context;
        this.dataSource = dataArgs;
        this.callback = callback;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_menu_item, parent, false);

        return new RecyclerViewHolder(view);
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout menuContainer;
        TextView menuItem;
        TextView status;
        SwitchCompat report;

        public RecyclerViewHolder(View view) {
            super(view);
            menuContainer = view.findViewById(R.id.menu_container);
            menuItem = view.findViewById(R.id.menu_item);
            status = view.findViewById(R.id.status);
            report = view.findViewById(R.id.report);
            reportSwitch = report;
            statusText = status;
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, final int position) {
        MenuItem data_provider = dataSource.get(position);
        holder.menuContainer.setTag(data_provider.getTag());
        holder.menuItem.setText(data_provider.getText());

        // FALL DETECTION STATUS COMPONENT ( IS IT ON OR OFF )
        if(data_provider.getTag().contains("fd")){

            //Standby Blinking
            Animation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(500); //You can manage the blinking time with this parameter
            anim.setStartOffset(20);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            holder.status.startAnimation(anim);
            callback.getStatus(holder.status);

        }
        // REPORT FALL COMPONENT
        else{
            holder.status.setVisibility(View.INVISIBLE);
            holder.report.setVisibility(View.VISIBLE);


        }


        holder.menuContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                if (callback != null) {
                    callback.onItemClicked(position);
                }
            }
        });

        callback.getSwitch(reportSwitch);

    }

    @Override
    public int getItemCount() {
        return dataSource.size();
    }
}

class MenuItem {
    private String text;
    private String tag;


    public MenuItem(String text, String tag) {
        this.text = text;
        this.tag = tag;
    }

    public String getText() {
        return text;
    }
    public String getTag() { return tag; }
}
