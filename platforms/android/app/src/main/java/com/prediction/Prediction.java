package com.prediction;

import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.tensorflow.lite.Interpreter;


public class Prediction extends CordovaPlugin {
    
    ArrayList<Interpreter> modelList = new ArrayList<>();
    float[] MODEL_WEIGHTS = {1.3464559f, -0.320784f, -1.4609069f, 1.6781534f};

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        modelList.add(new Interpreter(loadModelFile("10generic_adlone.tflite")));
        modelList.add(new Interpreter(loadModelFile("10generic_adltwo.tflite")));
        modelList.add(new Interpreter(loadModelFile("10generic_adlthree.tflite")));
        modelList.add(new Interpreter(loadModelFile("10generic_adlfour.tflite")));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("predict")) {

            JSONArray jsonArray = (JSONArray) args.get(0);
            float[][] newSamples = new float [jsonArray.length()][jsonArray.getJSONArray(0).length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                for (int j = 0; j < jsonArray.getJSONArray(0).length(); j++) {
                    newSamples[i][j] = (float) jsonArray.getJSONArray(i).getDouble(j);
                }
            }
////            cordova.getThreadPool().execute(new Runnable() {
//                public void run() {
//                    predict(newSamples, callbackContext);
//                }});
            predict(newSamples,callbackContext);

            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        for (Interpreter m : modelList) { m.close(); }
        super.onDestroy();
    }

    // transforms 2D array into 1D array
    public static float[] flatten_inputs(float[][] samples) {
        float[] flattenedSamples = new float[samples.length * samples[0].length];
        for (int i = 0; i < flattenedSamples.length; i += samples[0].length) {
            for (int x = 0; x < samples[0].length; x++) {
                flattenedSamples[i + x] = samples[ i / samples[0].length][x];
            }
        }
        return flattenedSamples;
    }


    private ByteBuffer loadModelFile(String fileName){

        try {
            AssetFileDescriptor fileDescriptor = cordova.getContext().getAssets().openFd(fileName);
            FileInputStream fileInputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = fileInputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            ByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset,declaredLength);
            return byteBuffer;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    public void predict(float[][] newSamples, CallbackContext callbackContext){
        float prediction = 0.0f;
        float[] flattendedSamples = flatten_inputs(newSamples);
        float[][]outputs =  new float[1][2];
        outputs[0][0] = 0f;
        outputs[0][1] = 0f;

        for (int i = 0 ; i < 4; i++) {
            modelList.get(i).run(flattendedSamples, outputs);   // make inference of beta samples
            prediction += outputs[0][0] * MODEL_WEIGHTS[i];
        }
//        Log.d("PREDICTION: ", String.valueOf(prediction));
        PluginResult result = new PluginResult(PluginResult.Status.OK, prediction);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);

    }
}