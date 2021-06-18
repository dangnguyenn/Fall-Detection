package com.example.wear;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class FallActivity extends FragmentActivity {

    String mNodeID;
    String docId;
    SwitchCompat sosSwitch;
    Button okBtn;
    Button notFallBtn;
    JSONObject message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fall);
        mNodeID = getIntent().getStringExtra("mNodeID");    //null when triggering by switch?
        docId = getIntent().getStringExtra("docId");

        SwitchCompat sosSwitch = (SwitchCompat) findViewById(R.id.sosSwitch);
        Button okBtn = (Button) findViewById(R.id.ok_button);
        Button notFallBtn = (Button) findViewById(R.id.not_fall_button);

        sosSwitch.setChecked(false);
        sosSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                message = new JSONObject();

                if (isChecked) {
                    //TODO contact carer + display message
                    Log.d("SOS SWITCH: ", "YES");
//                    sosSwitch.setChecked(false);

                    try {
                        message.put("message", "TP");
                        message.put("docId", docId);
                        Wearable.getMessageClient(getApplicationContext()).sendMessage(mNodeID, "/fall_label", message.toString().getBytes());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                else {
                    Log.d("SOS SWITCH: ", "NO");
                }
            }
        });

        okBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                message = new JSONObject();

                try {

                    message.put("message", "TP");
                    message.put("docId", docId);
                    Wearable.getMessageClient(getApplicationContext()).sendMessage(mNodeID, "/fall_label", message.toString().getBytes());
                    Log.e("Fall Activity Message Sent: ",message.toString());

                } catch (JSONException e) {
                        e.printStackTrace();
                }
                finish();
            }
        });

        notFallBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
             message = new JSONObject();
                try {

                    message.put("message", "FP");
                    message.put("docId", docId);
                    Wearable.getMessageClient(getApplicationContext()).sendMessage(mNodeID, "/fall_label", message.toString().getBytes());


                } catch (JSONException e) {
                        e.printStackTrace();
                }
                finish();
            }
        });
    }

}