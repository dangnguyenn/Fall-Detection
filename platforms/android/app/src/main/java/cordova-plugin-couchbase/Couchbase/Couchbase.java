package com.Couchbase;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.Array;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableArray;
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
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Cordova Couchbase-lite-2.8.0 Plugin
 *
 * This plugin enables your Cordova and PhoneGap mobile applications to use the Couchbase Lite 2.8.0.
 *
 */
public class Couchbase extends CordovaPlugin {


    static protected Database database = null;
    static protected DatabaseConfiguration config = null;
    private Context context;



    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getContext();
        CouchbaseLite.init(context);

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("createNewDatabase")) {
            String dbName = args.getString(0);
            this.createNewDatabase(dbName, callbackContext);
            return true;
        }

        else if (action.equals("insertDocument")) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {

                        String dbName = args.getString(0);
                        JSONObject document = args.getJSONObject(1);
                        insertDocument(dbName, document, callbackContext);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        }
        else if (action.equals("updateDocument")) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {

                        String dbName = args.getString(0);
                        String documentId = args.getString(1);
                        JSONObject updates = args.getJSONObject(2);
                        updateDocument(dbName, documentId, updates, callbackContext);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        }
        else if (action.equals("getAllDocuments")) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {

                        String dbName = args.getString(0);
                        getAllDocuments(dbName, callbackContext);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        }
        else if(action.equals("uploadDocuments")){
            try {

                URL url = new URL(args.getString(0));
                String dbName = args.getString(1);
                this.uploadDocuments(url, dbName, callbackContext);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    /**
     * Helper Function: getDatabaseSize
     *
     * @param {String} dbName - The name of the database you'd like to know the size of.
     *
     * @return {int} size of the database
     * */
    private long getDatabaseSize(String dbName){
        config = new DatabaseConfiguration();
        try {
            database = new Database(dbName, config);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return database.getCount();
    }

    /**
     * Function: createNewDatabase
     *
     * @param {String} dbName - The name of the database you'd like to create.
     * @param {successCallback} success - Success callback, called upon successful creation/opening of a database.
     * @param {failCallback} error - Error callback.
     *
     * */
    private void createNewDatabase(String dbName, CallbackContext callbackContext){

        try {

            config = new DatabaseConfiguration();
            database = new Database(dbName, config);

//            if(dbName.equals("accelerometer")) {
//                database.delete();
//            }

            callbackContext.success((int) database.getCount());

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

    }

    /**
     * Function: insertDocument
     *
     * @param {String} dbName - The name of the database you're inserting the document to.
     * @param {JSONObject} document - JSON Object document you're like to store in the database.
     * @param {successCallback} success - Success callback, called upon successful insertion of the document.
     * @param {failCallback} error - Error callback.
     *
     * */
    private void insertDocument(String dbName, JSONObject doc, CallbackContext callbackContext) throws JSONException {
        try {
            // open database
            config = new DatabaseConfiguration();
            database = new Database(dbName,config);

            // transform the json object to a document
            Map<String,Object> prop = new ObjectMapper().readValue(doc.toString(), HashMap.class);
            MutableDocument mutableDocument = new MutableDocument(prop);

            // save the document to the database
            database.save(mutableDocument);
            callbackContext.success(mutableDocument.getId());

        } catch (CouchbaseLiteException | IOException e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }

    }

    /**
     * Helper Function: updateDocument
     *
     * @param {String} dbName - The name of the database you'd like to know the size of.
     * @param {successCallback} success - Success callback, called upon successful creation/opening of a database.
     * @param {failCallback} error - Error callback.
     *
     * */
    private void updateDocument(String dbName, String documentID, JSONObject updates, CallbackContext callbackContext) {

        try {
            config = new DatabaseConfiguration();
            database = new Database(dbName,config);
            Document document = database.getDocument(documentID);
            MutableDocument mutableDocument = document.toMutable();

            Iterator<String> keys =  updates.keys();

            while(keys.hasNext()) {
                String key = keys.next();
                Object value = updates.get(key);
                if (value instanceof JSONArray) {
                    MutableArray mutableArray = new MutableArray();
                    JSONArray jsonArray = updates.getJSONArray(key);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        Object element =  jsonArray.get(i);
                        mutableArray.insertValue(i, element);
                    }
                    mutableDocument.setArray(key, mutableArray);
                }
                else
                    mutableDocument.setValue(key,value);
            }
            database.save(mutableDocument);
        } catch (CouchbaseLiteException | JSONException e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }

    }

    /**
     * Function: uploadDocuments
     *
     * @param {URL} url - The url hosting the php script for the remote couchbase database.
     * @param {String} dbName - The name of the database you're uploading the documents from.
     * @param {successCallback} success - Success callback, called when documents are uploaded successfully.
     * @param {failCallback} error - Error callback.
     *
     * */
    private void uploadDocuments(URL url, String dbName, CallbackContext callbackContext) {

        try {

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            ArrayList<JSONObject> objects = getAllDocuments(dbName,callbackContext);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.getOutputStream().write(objects.toString().getBytes());

            int responseCode = connection.getResponseCode();
            // if upload is successful purge all documents from the local database
            if (responseCode == HttpURLConnection.HTTP_OK) {

                for(JSONObject object: objects) {

                    database.purge(object.getString("id"));

                }
            }

        } catch (IOException | JSONException | CouchbaseLiteException e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }

    }

    /**
     * Function: getAllDocuments
     *
     * @param {String} dbName - The name of the database you're retrieving the documents from.
     * @param {successCallback} success - Success callback, returns a list of documents from the database.
     * @param {failCallback} error - Error callback.
     *
     * @return {ArrayList<JSONObject>} documents - a list of JSONObject documents.
     * */
    private ArrayList<JSONObject> getAllDocuments(String dbName, CallbackContext callbackContext) {

        String id = "";
        JSONObject jsonObject = null;
        JSONObject keys = null;
        ArrayList<JSONObject> documents = new ArrayList<>();
        //open database
        try {
            config = new DatabaseConfiguration();
            database = new Database(dbName,config);
            Query query = QueryBuilder.select(SelectResult.expression(Meta.id),SelectResult.all()).from(DataSource.database(database));
            ResultSet results = query.execute();

            for (Result result : results.allResults()) {

                id =  result.getValue("id").toString();
                jsonObject = new JSONObject();
                keys = new JSONObject(result.getDictionary(dbName).toMap());
                jsonObject.put("id", id);
                jsonObject.put("keys",keys);
                documents.add(jsonObject);
            }
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, documents.toString().getBytes());
            callbackContext.sendPluginResult(pluginResult);

        } catch (CouchbaseLiteException | JSONException e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }
        return documents;
    }

}