package com.WearOS;

import android.content.Context;
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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Cordova WearOS Plugin
 *
 * This plugin enables your Cordova and PhoneGap mobile applications to use the Google Play Services 17.0.0 Wearable API to
 * send and receive messages to and from a WearOS connected wearable.
 *
 */
public class WearOS extends CordovaPlugin {

    private Context context;
    private Node wNode;
//    private ArrayList<WearableListener> wearableListeners;
    WearableListener wListener;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getContext();
//        wearableListeners = new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.getMessageClient(context).removeListener(wListener);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("subscribe")) {

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    subscribe(callbackContext);
                }});
            return true;
        }
        else if (action.equals("sendMessage")) {

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        JSONObject jsonObject = args.getJSONObject(0);
                        String messagePath = args.getString(1);
                        sendMessage(jsonObject, messagePath, callbackContext);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }});
            return true;
        }
        return false;
    }

    /**
     * Helper Function: determineWearableNode
     *
     * This function helps determine the wearable node that we will be
     * sending messages too.
     *
     * */
    private void determineWearableNode() {

        try {

            Task<List<Node>> nodeListTask =  Wearable.getNodeClient(context).getConnectedNodes();

            List<Node> nodeList = Tasks.await(nodeListTask);

            for(Node node: nodeList) {
                if(node.isNearby()) {
                    wNode = node;
                    break;
                }
                wNode = node;
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function: subscribe
     *
     * This function adds a Wearable Listener to the Wearable Message Client and reports all received
     * messages and errors to the supplied callback functions.
     *
     * @param {successCallback} success - Success callback, called repeatedly
     * for each message recieved.
     * @param {failCallback} error - Error callback.
     *
     * */
    private void subscribe(CallbackContext callbackContext) {

        WearableListener wearableListener = new WearableListener(callbackContext);
        Wearable.getMessageClient(context).addListener(wearableListener);
//        wearableListeners.add(wearableListener);
        wListener = wearableListener;

    }


    /**
     * Function: sendMessage
     *
     * This function sends a JSONObject message to the connected wearable. All successes and errors are
     * reported to the supplied callback functions.
     *
     * @param {JSONObject} jsonObject - JSON Object message
     * @param {String} messagePath - Path to wear the message will be stored by the wearable.
     * @param {successCallback} success - Success callback, called when a message is sent successfully.
     * @param {failCallback} error - Error callback.
     *
     * */
    private void sendMessage(JSONObject jsonObject, String messagePath, CallbackContext callbackContext) {

            if (wNode == null)
                determineWearableNode();


            Wearable.getMessageClient(context).sendMessage(wNode.getId(), messagePath, jsonObject.toString().getBytes());
            Log.e("Message Sent: " ,jsonObject.toString());
            callbackContext.success();

    }

    /**
     * Class: WearableListener
     *
     *  A listener that implements the onMessageReceivedListener provided by google play services.
     *  Every time a message event is received the listener translate the message event into a JSONObject
     *  and sends it to the javascript provided success function via callbackContext.
     *
     * @param {CallbackContext} callbackContext - The success callback context that is sends a message in JSONObject to
     * provided javascript success function.
     *
     * */
    private class WearableListener implements DataClient.OnDataChangedListener,
            MessageClient.OnMessageReceivedListener,
            CapabilityClient.OnCapabilityChangedListener {

        private CallbackContext callbackContext;


        public WearableListener(CallbackContext callbackContext) {
            this.callbackContext = callbackContext;

        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
        }

        @Override
        public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {

        }

        @Override
        public void onMessageReceived(@NonNull MessageEvent messageEvent) {

            try {

                JSONObject jsonMessage = new JSONObject(new String(messageEvent.getData()));

                jsonMessage.put("path", messageEvent.getPath());

//                Log.d("Message Received: ", jsonMessage.toString());

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonMessage.toString());
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}