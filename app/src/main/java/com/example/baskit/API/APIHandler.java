package com.example.baskit.API;

import com.example.baskit.Firebase.FirebaseAuthHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import okhttp3.*;

public class APIHandler
{
    private static APIHandler instance;
    private static final String SERVER_URL = "http://10.0.2.2:5000";
    private static String firebaseToken;
    private final OkHttpClient client = new OkHttpClient();

    public APIHandler() {}

    public static APIHandler getInstance()
    {
        if (instance == null)
        {
            instance = new APIHandler();
        }

        return instance;
    }

    public void resetInstance()
    {
        firebaseToken = null;
        instance = null;
        instance = new APIHandler();
    }

    public void setFirebaseToken(String firebaseToken)
    {
        APIHandler.firebaseToken = firebaseToken;
        login();
    }

    private void login()
    {
        try
        {
            JSONObject body = new JSONObject();
            Request request = new Request.Builder()
                    .url(SERVER_URL + "/user")
                    .addHeader("FirebaseToken", firebaseToken)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);

            JSONObject obj = new JSONObject(response.body().string());

            response.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private String getRaw(String endpoint) throws IOException
    {
        Request request = new Request.Builder()
                .url(SERVER_URL + endpoint)
                .addHeader("FirebaseToken", firebaseToken)
                .build();

        try (Response response = client.newCall(request).execute())
        {
            return response.body().string();
        }
    }

    private void postRaw(String endpoint, String body) throws IOException {
        Request request = new Request.Builder()
                .url(SERVER_URL + endpoint)
                .addHeader("FirebaseToken", firebaseToken)
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful()) throw new IOException("POST failed: " + response);
        }
    }

    private String getBody(String name, ArrayList<String> arr) throws JSONException
    {
        JSONArray jsonArray = new JSONArray(arr);  // convert ArrayList<String> to JSONArray
        JSONObject body = new JSONObject().put(name, jsonArray);

        return body.toString();
    }

    // Cities
    public ArrayList<String> getCities() throws IOException, JSONException
    {
        String citiesRaw = getRaw("/cities");

        JSONArray citiesJson = new JSONArray(citiesRaw);
        ArrayList<String> cities = new ArrayList<>();

        for (int i = 0; i < citiesJson.length(); i++)
        {
            cities.add(citiesJson.getString(i));
        }

        return cities;
    }

    public void setCities(ArrayList<String> cities) throws IOException, JSONException
    {
        postRaw("/cities", getBody("cities", cities));
    }

    // Stores
    public ArrayList<String> getStores() throws IOException, JSONException
    {
        String storesRaw = getRaw("/stores");
        JSONArray storesJson = new JSONArray(storesRaw);
        ArrayList<String> stores = new ArrayList<>();

        for (int i = 0; i < storesJson.length(); i++) {
            stores.add(storesJson.getString(i));
        }
        return stores;
    }

    public void setStores(ArrayList<String> stores) throws IOException, JSONException
    {
        postRaw("/stores", getBody("stores", stores));
    }

    // Branches
    public Map<String, ArrayList<String>> getBranches() throws IOException, JSONException
    {
        String branchesRaw = getRaw("/branches");
        JSONObject branchesJson = new JSONObject(branchesRaw);
        Map<String, ArrayList<String>> branchesMap = new HashMap<>();

        for (Iterator<String> it = branchesJson.keys(); it.hasNext();)
        {
            String store = it.next();
            JSONArray arr = branchesJson.getJSONArray(store);
            ArrayList<String> branchList = new ArrayList<>();

            for (int i = 0; i < arr.length(); i++)
            {
                branchList.add(arr.getString(i));
            }
            branchesMap.put(store, branchList);
        }

        return branchesMap;
    }

    public void setBranches(Map<String, ArrayList<String>> branches) throws IOException, JSONException
    {
        JSONObject body = new JSONObject();

        for (Map.Entry<String, ArrayList<String>> entry : branches.entrySet())
        {
            JSONArray arr = new JSONArray(entry.getValue()); // convert ArrayList to JSONArray
            body.put(entry.getKey(), arr); // now it will serialize as ["a","b","c"]
        }

        postRaw("/branches", body.toString());
    }

    // Items
    public String getItemName(String itemCode) throws IOException
    {
        String endpoint = "/item_name?"
                + "item_code=" + URLEncoder.encode(itemCode, "UTF-8");
        return getRaw(endpoint);
    }

    public String getItemCode(String itemName) throws IOException
    {
        String endpoint = "/item_code?"
                + "item_name=" + URLEncoder.encode(itemName, "UTF-8");
        return getRaw(endpoint);
    }

    public Map<String, Map<String, Double>> getItemPricesByCode(String itemCode) throws IOException, JSONException
    {
        String endpoint = "/item_prices?"
                + "item_code=" + URLEncoder.encode(itemCode, "UTF-8");
        return parsePriceResponse(getRaw(endpoint));
    }

    public Map<String, Map<String, Double>> getItemPricesByName(String itemName) throws IOException, JSONException
    {
        String endpoint = "/item_prices?"
                + "item_name=" + URLEncoder.encode(itemName, "UTF-8");
        return parsePriceResponse(getRaw(endpoint));
    }

    private Map<String, Map<String, Double>> parsePriceResponse(String response) throws JSONException
    {
        JSONObject json = new JSONObject(response);
        Map<String, Map<String, Double>> result = new HashMap<>();

        for (Iterator<String> it = json.keys(); it.hasNext();)
        {
            String store = it.next();
            JSONObject branches = json.getJSONObject(store);
            Map<String, Double> branchMap = new HashMap<>();

            for (Iterator<String> iter = branches.keys(); iter.hasNext();)
            {
                String branch = iter.next();
                branchMap.put(branch, branches.getDouble(branch));
            }

            result.put(store, branchMap);
        }

        return result;
    }
}