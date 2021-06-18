package com.wearOS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.ExecutionException;


public class WearOS extends CordovaPlugin {

    Context context;
    private Activity activity;
    DataClient dataClient;
    Wearable.WearableOptions options;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getContext();
        activity = cordova.getActivity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("subscribe")) {

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    String dataLayerPath = null, dataLayerType = null;
                    try {
                        dataLayerPath = args.getString(0);
                        dataLayerType = args.getString(1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    subscribe(dataLayerPath,dataLayerType,callbackContext);
                }});
            return true;
        }
        else if (action.equals("sendMessage")){

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    JSONObject jsonObject = null;
                    String messagePath = null;
                    try {
                        jsonObject = args.getJSONObject(0);
                        messagePath = args.getString(1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sendMessage(jsonObject,messagePath,callbackContext);
                }});
            return true;
        }
        return false;
    }


    private void subscribe(String dataLayerPath,String dataLayerType,CallbackContext callbackContext){

        Wearable.getDataClient(context).addListener(new WearableListener(dataLayerPath,dataLayerType,callbackContext));
    }



    private void sendMessage(JSONObject jsonObject, String messagePath, CallbackContext callbackContext){

        Task<List<Node>> nodeListTask =  Wearable.getNodeClient(context).getConnectedNodes();

        try {
            List<Node> nodeList = Tasks.await(nodeListTask);

            for(Node node: nodeList){
                Task<Integer> sendMessageTask =
                        Wearable.getMessageClient(context).sendMessage(node.getId(), messagePath, jsonObject.toString().getBytes());

                Integer result = Tasks.await(sendMessageTask);
                callbackContext.success(result);

            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class WearableListener implements DataClient.OnDataChangedListener,
            MessageClient.OnMessageReceivedListener,
            CapabilityClient.OnCapabilityChangedListener{

        private CallbackContext callbackContext;
        private String dataLayerPath, dataLayerType;

        public WearableListener(String dataLayerPath, String dataLayerType, CallbackContext callbackContext){
            this.callbackContext = callbackContext;
            this.dataLayerPath = dataLayerPath;
            this.dataLayerType = dataLayerType;
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent event : dataEventBuffer) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    // DataItem changed
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo(dataLayerPath) == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                        try {
                            JSONObject object = new JSONObject(new String(dataMap.getByteArray(dataLayerType)));
                            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, object.toString());
                            pluginResult.setKeepCallback(true);
                            callbackContext.sendPluginResult(pluginResult);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }

        @Override
        public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {

        }

        @Override
        public void onMessageReceived(@NonNull MessageEvent messageEvent) {

        }
    }

}
