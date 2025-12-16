package com.example.baskit.API;

import android.util.Log;

import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.SQLite.AppDatabase;
import com.example.baskit.SQLite.ItemCodeName;
import com.example.baskit.SQLite.ItemEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.*;
import java.util.concurrent.TimeUnit;

public class APIHandler
{
    private static APIHandler instance;
    private static final String PRIVATE_NETWORK_URL = "192.168.1.204";
    private static final String EMULATOR_URL = "10.0.2.2";

    private static final String SERVER_URL = "http://" + EMULATOR_URL + ":5001";
    private static String firebaseToken;
    private static Map<String, Map<String, Map<String, Double>>> allItems;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .build();
    private Map<String, Map<String, Map<String, Double>>> cachedItems = null;
    private Map<String, String> cachedCodeNames = null;
    private ArrayList<Supermarket> supermarkets = null;

    private APIHandler() {}

    public static APIHandler getInstance()
    {
        if (instance == null)
        {
            instance = new APIHandler();
        }

        return instance;
    }

    public void preload() throws JSONException, IOException
    {
        supermarkets = getUpdatedSupermarkets();
        cachedItems = loadItemsFromDB();
        cachedCodeNames = loadCodeNamesFromDB();

        if (cachedItems.isEmpty())
        {
            try
            {
                Map<String, Map<String, Map<String, Double>>> freshItems = getItemsFromAPI();
                Map<String, String> freshCodeNames = getItemsCodeNameFromAPI(new ArrayList<>(freshItems.keySet()));

                updateCache(freshItems, freshCodeNames);
                saveItemsToDB(freshItems);
                saveCodeNamesToDB(freshCodeNames);
            }
            catch (Exception e)
            {
                Log.e("API Preload", e.getMessage());
            }
        }
        else
        {
            try
            {
                new Thread(() ->
                {
                    Map<String, Map<String, Map<String, Double>>> freshItems = getItemsFromAPI();
                    Map<String, String> freshCodeNames = getItemsCodeNameFromAPI(new ArrayList<>(freshItems.keySet()));

                    updateCache(freshItems, freshCodeNames);
                    saveItemsToDB(freshItems);
                    saveCodeNamesToDB(freshCodeNames);
                }).start();
            }
            catch (Exception e)
            {
                Log.e("API load new data", e.getMessage());
            }
        }
    }

    private Map<String, Map<String, Map<String, Double>>> loadItemsFromDB()
    {
        List<ItemEntity> dbItems = AppDatabase.getDatabase(Baskit.getContext())
                .itemDao()
                .getAll();

        Map<String, Map<String, Map<String, Double>>> result = new HashMap<>();

        for (ItemEntity entity : dbItems)
        {
            result.computeIfAbsent(entity.itemCode, k -> new HashMap<>())
                    .computeIfAbsent(entity.store, k -> new HashMap<>())
                    .put(entity.branch, entity.price);
        }

        return result;
    }

    private Map<String, String> loadCodeNamesFromDB()
    {
        List<ItemCodeName> dbCodes = AppDatabase.getDatabase(Baskit.getContext())
                .itemCodesDao()
                .getAll();

        Map<String, String> codeNames = new HashMap<>();

        for (ItemCodeName code : dbCodes)
        {
            codeNames.put(code.getItemCode(), code.getItemName());
        }

        return codeNames;
    }

    private void updateCache(Map<String, Map<String, Map<String, Double>>> freshItems,
                             Map<String, String> freshCodeNames)
    {
        cachedItems.clear();
        cachedItems.putAll(freshItems);

        cachedCodeNames.clear();
        cachedCodeNames.putAll(freshCodeNames);
    }

    private void saveItemsToDB(Map<String, Map<String, Map<String, Double>>> freshItems)
    {
        new Thread(() ->
        {
            AppDatabase db = AppDatabase.getDatabase(Baskit.getContext());
            db.itemDao().clearAll();

            List<ItemEntity> itemsToInsert = new ArrayList<>();

            for (String itemCode : freshItems.keySet())
            {
                Map<String, Map<String, Double>> freshStores = freshItems.get(itemCode);

                for (String store : freshStores.keySet())
                {
                    Map<String, Double> freshBranches = freshStores.get(store);

                    for (String branch : freshBranches.keySet())
                    {
                        double freshPrice = freshBranches.get(branch);
                        ItemEntity entity = new ItemEntity();
                        entity.itemCode = itemCode;
                        entity.store = store;
                        entity.branch = branch;
                        entity.price = freshPrice;
                        itemsToInsert.add(entity);
                    }
                }
            }

            if (!itemsToInsert.isEmpty())
            {
                db.itemDao().insertAll(itemsToInsert);
            }
        }).start();
    }

    private void saveCodeNamesToDB(Map<String, String> freshCodeNames)
    {
        new Thread(() ->
        {
            AppDatabase db = AppDatabase.getDatabase(Baskit.getContext());
            db.itemCodesDao().clearAll();

            List<ItemCodeName> codesToInsert = new ArrayList<>();
            for (Map.Entry<String, String> entry : freshCodeNames.entrySet())
            {
                String code = entry.getKey();
                String name = entry.getValue();
                codesToInsert.add(new ItemCodeName(code, name));
            }

            if (!codesToInsert.isEmpty())
            {
                db.itemCodesDao().insertAll(codesToInsert);
            }
        }).start();
    }

    public Map<String, Map<String, Map<String, Double>>> getItems()
    {
        return cachedItems;
    }

    public Map<String, String> getItemsCodeName()
    {
        return cachedCodeNames;
    }

    private Map<String, Map<String, Map<String, Double>>> getItemsFromAPI()
    {
        Map<String, Map<String, Map<String, Double>>> items = new HashMap<>();

        try
        {
            String itemsRaw = getRaw("/items");
            JSONObject itemsJson = new JSONObject(itemsRaw);

            for (Iterator<String> itemIter = itemsJson.keys(); itemIter.hasNext();)
            {
                String itemCode = itemIter.next();
                JSONObject storesJson = itemsJson.getJSONObject(itemCode);
                Map<String, Map<String, Double>> storeMap = parsePriceResponse(storesJson.toString());

                items.put(itemCode, storeMap);
            }
        }
        catch (Exception e)
        {
            Log.e("API", e.getMessage());
        }

        return items;
    }

    private Map<String, String> getItemsCodeNameFromAPI(List<String> keys)
    {
        Map<String, String> codeNames = new HashMap<>();
        List<String> missing = new ArrayList<>();

        for (String key : keys)
        {
            if (!codeNames.containsKey(key))
            {
                missing.add(key);
            }
        }

        if (missing.isEmpty())
        {
            return codeNames;
        }

        try
        {
            JSONObject body = new JSONObject();
            body.put("item_codes", new JSONArray(missing));

            String responseRaw = postRawWithResponse("/items_code_name", body.toString());
            JSONObject jsonResponse = new JSONObject(responseRaw);

            for (Iterator<String> it = jsonResponse.keys(); it.hasNext();)
            {
                String code = it.next();
                String name = jsonResponse.getString(code);
                codeNames.put(code, name);
            }
        }
        catch (Exception e)
        {
            Log.e("API", e.getMessage());
        }

        return codeNames;
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

            try (Response response = client.newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    return;
                }

                ResponseBody responseBody = response.body();
            }
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
            if (!response.isSuccessful()) {
                throw new IOException("GET failed: " + response.code() + " " + response.message());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Empty response body for GET " + endpoint);
            }

            return responseBody.string();
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
    public ArrayList<String> getAllCities() throws IOException, JSONException
    {
        String citiesRaw = getRaw("/all_cities");

        JSONArray citiesJson = new JSONArray(citiesRaw);
        ArrayList<String> cities = new ArrayList<>();

        for (int i = 0; i < citiesJson.length(); i++)
        {
            cities.add(citiesJson.getString(i));
        }

        return cities;
    }

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

    public Map<String, ArrayList<String>> getChoices() throws IOException, JSONException
    {
        String branchesRaw = getRaw("/choices");
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

    public ArrayList<Supermarket> getSupermarkets()
    {
        return supermarkets;
    }

    public boolean singleSectionInSupermarkets(Supermarket supermarket)
    {
        for (Supermarket sm : supermarkets)
        {
            if (!sm.equals(supermarket) && sm.getSupermarket().equals(supermarket.getSupermarket()))
            {
                return false;
            }
        }

        return true;
    }

    public void updateSupermarkets() throws JSONException, IOException
    {
        ArrayList<Supermarket> supermarketsNew = new ArrayList<>();
        Map<String, ArrayList<String>> branches = getChoices();

        for (Map.Entry<String, ArrayList<String>> supermarketEntry : branches.entrySet())
        {
            String supermarketName = supermarketEntry.getKey();

            for (String section : supermarketEntry.getValue())
            {
                Supermarket supermarket = new Supermarket(supermarketName, section);
                supermarketsNew.add(supermarket);
            }
        }

        this.supermarkets = supermarketsNew;
    }

    public void updateSupermarkets(ArrayList<Supermarket> supermarketsNew)
    {
        this.supermarkets = supermarketsNew;
    }

    public ArrayList<Supermarket> getUpdatedSupermarkets() throws JSONException, IOException
    {
        updateSupermarkets();
        return getSupermarkets();
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

    private String postRawWithResponse(String endpoint, String body) throws IOException
    {
        Request request = new Request.Builder()
                .url(SERVER_URL + endpoint)
                .addHeader("FirebaseToken", firebaseToken)
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful()) throw new IOException("POST failed: " + response);

            ResponseBody rb = response.body();
            if (rb == null) throw new IOException("Empty response body");

            return rb.string();
        }
    }
}