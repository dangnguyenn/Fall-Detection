package com.Couchbase;

import android.app.Activity;
import android.content.Context;

import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class echoes a string called from JavaScript.
 */
public class Couchbase extends CordovaPlugin {


    static protected Database database = null;
    static protected DatabaseConfiguration config = null;
    Context context;
    private Activity activity;
    private String uuid;



    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getContext();
        activity = cordova.getActivity();
        CouchbaseLite.init(context);

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {


       if (action.equals("createNewDatabase")){
           String dbName = args.getString(0);
           this.createNewDatabase(dbName,callbackContext);
       }

       else if (action.equals("insertDocument")){
           String dbName = args.getString(0);
           JSONObject doc = args.getJSONObject(1);
           this.insertDocument(dbName,doc,callbackContext);
       }
       else if (action.equals("query")){
           String dbName = args.getString(0);
           this.getAllDocuments(dbName,callbackContext);
       }
       else if(action.equals("uploadDocuments")){

           URL url = null;
           try {
               url = new URL(args.getString(0));
           } catch (MalformedURLException e) {
               e.printStackTrace();
           }
           //JSONObject doc = args.getJSONObject(1);

           this.uploadDocuments(url,callbackContext);
       }
        return false;
    }

    private long getDatabaseSize(String dbName, CallbackContext callbackContext){
        config = new DatabaseConfiguration();
        try {
            database = new Database(dbName, config);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return database.getCount();
    }



    private void createNewDatabase(String dbName, CallbackContext callbackContext){
        config = new DatabaseConfiguration();

        try {
            database = new Database(dbName,config);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        callbackContext.success((int) database.getCount());

    }


    private void insertDocument(String dbName,JSONObject doc, CallbackContext callbackContext) throws JSONException {


        config = new DatabaseConfiguration();

        //open database
        try {
            database = new Database(dbName,config);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        //map json doc to map
        Map<String,Object> prop = null;
        try {
             prop = new ObjectMapper().readValue(doc.toString(), HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Create the document
        MutableDocument mutableDocument = new MutableDocument(prop);

        try {
            database.save(mutableDocument);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        callbackContext.success(mutableDocument.getId());


    }




    private void uploadDocuments(URL url, CallbackContext callbackContext) {

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            connection.setRequestMethod("POST");

        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        ArrayList<JSONObject> objects = getAllDocuments("userlogs",callbackContext);


        connection.setDoOutput(true);
        connection.setRequestProperty("Accept-Charset", "UTF-8");

        try {
            connection.getOutputStream().write(objects.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // if upload is successful purge all documents from the local database
        if (responseCode == HttpURLConnection.HTTP_OK){
            for(JSONObject object: objects) {
                try {
                    database.purge(objects.get(0).getString("id"));
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }



    }

    private ArrayList<JSONObject> getAllDocuments(String dbName,CallbackContext callbackContext){

        config = new DatabaseConfiguration();

        //open database
        try {
            database = new Database(dbName,config);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        Query query = QueryBuilder.select(SelectResult.expression(Meta.id),SelectResult.all()).from(DataSource.database(database));
        ResultSet results = null;
        try {
            results = query.execute();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        String id="";
        JSONObject jsonObject = null;
        JSONObject keys = null;
        ArrayList<JSONObject> objects = new ArrayList<>();

        for (Result result : results.allResults()){
            id =  result.getValue("id").toString();

            jsonObject = new JSONObject();
            keys = new JSONObject(result.getDictionary(dbName).toMap());

            try {
                jsonObject.put("id", id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                jsonObject.put("keys",keys);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            callbackContext.success(jsonObject.toString());
            objects.add(jsonObject);
        }

        return objects;

    }


}
