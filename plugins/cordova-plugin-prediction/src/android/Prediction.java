package com.prediction;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.tensorflow.lite.Interpreter;


public class Prediction extends CordovaPlugin {

    Interpreter tflite;

    private Queue<ArrayList<float[]>> alphaQueue = new LinkedList<ArrayList<float[]>>();
    private ConcurrentLinkedDeque<float[]> betaQueue;
    private ConcurrentLinkedDeque<float[]> heuristicsQueue;
    private int sampleLimit;
    private int alphaLimit;
    private int numDataPoints;
    

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        tflite = new Interpreter(loadModelFile());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("predict")) {
            float[] newSamples = (float[]) args.get(0);
            predict(newSamples,callbackContext);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        tflite.close();
        super.onDestroy();
    }

    // transforms 2D array into 1D array
    public static float[] flatten_inputs(ArrayList<float[]> samples) {
        float[] flattenedSamples = new float[samples.size() * samples.get(0).length];
        for (int i = 0; i < flattenedSamples.length; i += samples.get(0).length) {
            for (int x = 0; x < samples.get(0).length; x++) {
                flattenedSamples[i + x] = samples.get( i / samples.get(0).length)[x];
            }
        }
        return flattenedSamples;
    }


    private ByteBuffer loadModelFile(){

        try {
            AssetFileDescriptor fileDescriptor = cordova.getContext().getAssets().openFd("model.tflite");
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
    public float predict(float[] newSample, CallbackContext callbackContext){
       
        float output = 0.0f;
        
        try {
            // alpha limit is 20; send the alpha queue and averaged heuristics to cordova then the pop oldest
            if ( alphaQueue.size() >= alphaLimit ){
                // average the heuristics
                Float [] heuristics = new Float[heuristicsQueue.size()];
                heuristics = heuristicsQueue.toArray(heuristics);
                for(float heuristic : heuristics){
                    output += heuristic;
                }
                output = output / alphaLimit;
                
                //TODO send alpha queue and output throw callbackContext
                return output;
            }
            
            enqueueSample(newSample);
            
            // logic to determine if we can make the prediction ( is the alpha full ?? )
            
            //  if so make the prediction - average the 20 Heuristic from the beta's in the alpha
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return output;
    }

    // add the new sample to the betaQueue
    public boolean enqueueSample(float[] newSample) throws IOException {
        
            // if beta queue is full (35 samples):
            //          - push to alpha
            //          - make and push inference to heuristics
            //          - remove oldest from beta and add the new sample
            if (betaQueue.size() >= this.sampleLimit) {
                
                enqueueBeta(); // push to alpha, make and push inference to heuristics
                betaQueue.poll(); // remove oldest from beta
            }
            betaQueue.offer(newSample); // add the new sample to beta
            return true;
    }

    // needs to add the beta to alpha, needs to predict and add beta heuristics to the heuristics
    public void enqueueBeta(){
        
        ConcurrentLinkedDeque<float[]> tempQueue = new ConcurrentLinkedDeque<>(betaQueue);
        ArrayList<float[]> betaQueueSamples = new ArrayList<>();
        for(int i = 0; i < this.sampleLimit; i++) {
            betaQueueSamples.add(tempQueue.poll()); 
        }
        
        alphaQueue.add(betaQueueSamples); // add beta to alpha
        
        float[] flattendedSamples = flatten_inputs(betaQueueSamples); 
        float[][]outputs =  new float[1][1];
        outputs[0][0] = 0f;
        
        tflite.run(flattendedSamples,outputs); // make inference of beta samples
        heuristicsQueue.add(outputs[0]); // push beta inference to heuristics
    }
    
    // if TP/FP
    public void reset(){
        
        alphaQueue.clear();
        betaQueue.clear();
        heuristicsQueue.clear();
        
    }

}