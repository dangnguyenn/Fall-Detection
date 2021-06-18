package com.example.wear;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.example.wear.R;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends FragmentActivity implements  AmbientModeSupport.AmbientCallbackProvider, MessageClient.OnMessageReceivedListener {

    TextView myText;
    SwitchCompat switchCompat;
    TextView statusText;
    Intent serviceIntent;
    WearableRecyclerView recyclerView;
    ArrayList<MenuItem> menuItems;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        AmbientModeSupport.attach(this);
        Wearable.getMessageClient(this).addListener(this);
        serviceIntent = new Intent(this, DataCollectionService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        recyclerView = findViewById(R.id.main_menu_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);

        CustomScrollingLayoutCallback customScrollingLayoutCallback = new CustomScrollingLayoutCallback();
        recyclerView.setLayoutManager(new WearableLinearLayoutManager(this, customScrollingLayoutCallback));

        //recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));
        menuItems = new ArrayList<>();
        menuItems.add(new MenuItem( "Fall Detection","fd"));
        menuItems.add(new MenuItem("Report Fall","rf"));
        recyclerView.smoothScrollToPosition(3);
        recyclerView.setAdapter(new MainMenuAdapter(this, menuItems, new MainMenuAdapter.AdapterCallback() {

            @Override
            public void onItemClicked(Integer menuPosition) { }
            @Override
            public void getSwitch(SwitchCompat reportSwitch) {
                switchCompat = reportSwitch;
                switchClick(reportSwitch);
            }
            @Override
            public void getStatus(TextView statusTexts) {
                statusText = statusTexts;
            }
        }));

    }

    @Override
    protected void onResume() {
        if (switchCompat != null)
            switchCompat.setChecked(false);
        super.onResume();

    }

    public void switchClick(SwitchCompat switchCompat) {


        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    Intent intent = new Intent(MainActivity.this, FallActivity.class);
                    startActivity(intent);

                }
                else {
                    Log.d("switchCompat: ", "NO");
                }

            }
            });
    }



    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        try {
            JSONObject jsonObject = new JSONObject(new String(messageEvent.getData()));
            Log.d("Message from phone", "onMessageReceived: "+ jsonObject.toString());
            String message = jsonObject.getString("message");

            if(message.equals("start")){
                Log.d("start sensor", "In main ");
                statusText.setText("ON");
                statusText.setBackgroundColor(Color.GREEN);

            }
            else if(message.equals("fall")){
                Intent intent = new Intent(this, FallActivity.class);
                intent.putExtra("mNodeID", messageEvent.getSourceNodeId());
                intent.putExtra("docId", jsonObject.getString("docId"));
                startActivity(intent);

            }

        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    /** Customizes appearance for Ambient mode. (We don't do anything minus default.) */
    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        /** Prepares the UI for ambient mode. */
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            super.onEnterAmbient(ambientDetails);
        }

        /**
         * Updates the display in ambient mode on the standard interval. Since we're using a custom
         * refresh cycle, this method does NOT update the data in the display. Rather, this method
         * simply updates the positioning of the data in the screen to avoid burn-in, if the display
         * requires it.
         */
        @Override
        public void onUpdateAmbient() {
            super.onUpdateAmbient();
        }

        /** Restores the UI to active (non-ambient) mode. */
        @Override
        public void onExitAmbient() {
            super.onExitAmbient();
        }
    }
}