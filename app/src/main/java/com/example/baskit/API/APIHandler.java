package com.example.baskit.API;

import android.util.Log;

import com.example.baskit.Baskit;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.SQLite.AppDatabase;
import com.example.baskit.SQLite.ItemCodeName;
import com.example.baskit.SQLite.ItemCategory;
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

import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import okhttp3.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class APIHandler
{
    private static APIHandler instance;
    private static final String PRIVATE_NETWORK_URL = "172.20.10.13";
    private static final String EMULATOR_URL = "10.0.2.2";

    private static final String SERVER_URL = "http://" + EMULATOR_URL + ":5001";
    private static String firebaseToken;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build();
    private Map<String, Map<String, Map<String, Double>>> cachedItems = null;
    private Map<String, String> cachedCodeNames = null;
    private Map<String, String> cachedItemCategories = new HashMap<>();
    private ArrayList<Supermarket> supermarkets = null;

    // Shared executors
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(3);
    private final ExecutorService geminiExecutor = Executors.newFixedThreadPool(3);

    private APIHandler() {}

    public static APIHandler getInstance()
    {
        if (instance == null)
        {
            instance = new APIHandler();
        }

        return instance;
    }

    public boolean isServerActive()
    {
        try
        {
            Request request = new Request.Builder()
                    .url(SERVER_URL + "/active")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute())
            {
                return response.isSuccessful();
            }
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public void reset() throws JSONException, IOException
    {
        cachedItems = null;
        cachedCodeNames = null;
        cachedItemCategories.clear();

        preload();
    }

    public void preload() throws JSONException, IOException
    {
        supermarkets = getUpdatedSupermarkets();
        cachedItems = loadItemsFromDB();
        cachedCodeNames = loadCodeNamesFromDB();
        cachedItemCategories = loadCategoriesFromDB();

        if (cachedItems.isEmpty() || cachedCodeNames.isEmpty())
        {
            try
            {
                Map<String, Map<String, Map<String, Double>>> freshItems = getItemsFromAPI();
                Map<String, String> freshCodeNames = getItemsCodeNameFromAPI(new ArrayList<>(freshItems.keySet()));

                updateCache(freshItems, freshCodeNames);
                saveItemsToDB(freshItems);
                saveCodeNamesToDB(freshCodeNames);
                updateCategoriesAfterRefresh(freshItems);
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
                networkExecutor.execute(() ->
                {
                    try
                    {
                        Map<String, Map<String, Map<String, Double>>> freshItems = getItemsFromAPI();
                        Map<String, String> freshCodeNames = getItemsCodeNameFromAPI(new ArrayList<>(freshItems.keySet()));

                        updateCache(freshItems, freshCodeNames);
                        saveItemsToDB(freshItems);
                        saveCodeNamesToDB(freshCodeNames);
                        updateCategoriesAfterRefresh(freshItems);
                    }
                    catch (Exception e)
                    {
                        Log.e("API Background Refresh", e.getMessage());
                    }
                });
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
        dbExecutor.execute(() ->
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
        });
    }

    private void saveCodeNamesToDB(Map<String, String> freshCodeNames)
    {
        dbExecutor.execute(() ->
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
        });
    }

    public Map<String, Map<String, Map<String, Double>>> getItems()
    {
        return cachedItems;
    }

    public Map<String, String> getItemsCodeName()
    {
        return cachedCodeNames;
    }

    public String getItemCategoryDB(String itemCode)
    {
        if (itemCode == null) return null;
        return cachedItemCategories.get(itemCode);
    }

    public void putItemCategoryIfAbsent(String itemCode, String category)
    {
        if (itemCode == null || category == null) return;

        String normalized = itemCode.trim().toLowerCase();

        if (!cachedItemCategories.containsKey(normalized))
        {
            cachedItemCategories.put(normalized, category);
        }
    }

    public boolean hasCategory(String itemCode)
    {
        if (itemCode == null) return false;
        return cachedItemCategories.containsKey(itemCode.trim().toLowerCase());
    }

    private Map<String, String> loadCategoriesFromDB()
    {
        List<ItemCategory> dbCategories = AppDatabase.getDatabase(Baskit.getContext())
                .itemCategoryDao()
                .getAll();

        Map<String, String> categories = new HashMap<>();

        for (ItemCategory category : dbCategories)
        {
            categories.put(category.getItemCode(), category.getCategory());
        }

        return categories;
    }

    private void saveCategoriesToDB(Map<String, String> categories)
    {
        dbExecutor.execute(() ->
        {
            AppDatabase db = AppDatabase.getDatabase(Baskit.getContext());

            List<ItemCategory> list = new ArrayList<>();

            for (Map.Entry<String, String> entry : categories.entrySet())
            {
                list.add(new ItemCategory(entry.getKey(), entry.getValue()));
            }

            if (!list.isEmpty())
            {
                db.itemCategoryDao().insertAll(list);
            }
        });
    }

    private Map<String, String> callGeminiForCategories(
            Map<String, String> codeToName) throws Exception
    {
        List<String> itemNames = new ArrayList<>(codeToName.values());

        String prompt = com.example.baskit.AI.AIHandler.getInstance()
                .createBatchCategoryPromptFromNames(itemNames); // <-- new prompt method

        String rawResult = com.example.baskit.AI.AIHandler.getInstance()
                .classifyBatchBlocking(prompt);

        if (rawResult != null)
        {
            rawResult = rawResult.trim();

            if (rawResult.startsWith("```"))
            {
                rawResult = rawResult.replaceAll("(?s)```json", "")
                                     .replaceAll("(?s)```", "")
                                     .trim();
            }
        }

        Map<String, String> resultMap = new HashMap<>();

        if (rawResult == null || rawResult.isEmpty())
        {
            Log.e("AI_BATCH", "Empty Gemini response (quota/timeout)");
            return resultMap;
        }

        try
        {
            int firstBrace = rawResult.indexOf("{");
            int lastBrace = rawResult.lastIndexOf("}");

            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace)
            {
                rawResult = rawResult.substring(firstBrace, lastBrace + 1);
            }

            int openBraces = 0;
            int closeBraces = 0;
            int openBrackets = 0;
            int closeBrackets = 0;

            for (char c : rawResult.toCharArray())
            {
                if (c == '{') openBraces++;
                if (c == '}') closeBraces++;
                if (c == '[') openBrackets++;
                if (c == ']') closeBrackets++;
            }

            StringBuilder balanced = new StringBuilder(rawResult);

            while (closeBrackets < openBrackets)
            {
                balanced.append("]");
                closeBrackets++;
            }

            while (closeBraces < openBraces)
            {
                balanced.append("}");
                closeBraces++;
            }

            rawResult = balanced.toString();
            rawResult = rawResult.replace(",]", "]");
            rawResult = rawResult.replace(", }", " }");
            rawResult = rawResult.replace(",}", "}");

            String repaired = rawResult;
            JSONObject rootObject = null;

            for (int attempt = 0; attempt < 5; attempt++)
            {
                try
                {
                    rootObject = new JSONObject(repaired);
                    break;
                }
                catch (JSONException ex)
                {
                    if (repaired.length() > 1)
                    {
                        repaired = repaired.substring(0, repaired.length() - 1).trim();
                    }
                }
            }

            if (rootObject == null)
            {
                Log.e("AI_BATCH", "Unable to repair malformed Gemini JSON after retries");
                return resultMap;
            }

            Map<String, String> nameToCode = new HashMap<>();
            for (Map.Entry<String, String> entry : codeToName.entrySet())
            {
                nameToCode.put(entry.getValue(), entry.getKey());
            }

            Iterator<String> categoryKeys = rootObject.keys();
            while (categoryKeys.hasNext())
            {
                String category = categoryKeys.next();

                try
                {
                    JSONArray arr = rootObject.getJSONArray(category);

                    for (int i = 0; i < arr.length(); i++)
                    {
                        try
                        {
                            String name = arr.getString(i);
                            String code = nameToCode.get(name);

                            if (code != null)
                            {
                                resultMap.put(code.trim().toLowerCase(), category);
                            }
                        }
                        catch (Exception ignored) {}
                    }
                }
                catch (Exception ignored) {}
            }
        }
        catch (Exception e)
        {
            Log.e("AI_BATCH", "Failed to parse Gemini JSON. Raw response: " + rawResult, e);
        }

        return resultMap;
    }

    private void updateCategoriesAfterRefresh(Map<String, Map<String, Map<String, Double>>> freshItems)
    {
        networkExecutor.execute(() ->
        {
            try
            {
                Map<String, String> missingCodeToName = new HashMap<>();

                for (String itemCode : freshItems.keySet())
                {
                    String normalized = itemCode.trim().toLowerCase();

                    if (!cachedItemCategories.containsKey(normalized))
                    {
                        String itemName = cachedCodeNames.get(itemCode);
                        if (itemName != null)
                        {
                            missingCodeToName.put(normalized, itemName);
                        }
                    }
                }

                if (missingCodeToName.isEmpty())
                {
                    return;
                }

                final int CHUNK_SIZE = 40;

                List<String> codes = new ArrayList<>(missingCodeToName.keySet());

                List<Future<Map<String, String>>> futures = new ArrayList<>();

                for (int start = 0; start < codes.size(); start += CHUNK_SIZE)
                {
                    int end = Math.min(start + CHUNK_SIZE, codes.size());

                    Map<String, String> chunkMap = new HashMap<>();
                    for (int i = start; i < end; i++)
                    {
                        String code = codes.get(i);
                        chunkMap.put(code, missingCodeToName.get(code));
                    }

                    Future<Map<String, String>> future = geminiExecutor.submit(() ->
                    {
                        try
                        {
                            return callGeminiForCategories(chunkMap);
                        }
                        catch (Exception e)
                        {
                            Log.e("CATEGORY_REFRESH", "Gemini chunk failed", e);
                            return new HashMap<>();
                        }
                    });

                    futures.add(future);
                }

                for (Future<Map<String, String>> future : futures)
                {
                    try
                    {
                        Map<String, String> results = future.get();
                        cachedItemCategories.putAll(results);
                        saveCategoriesToDB(results);
                    }
                    catch (Exception e)
                    {
                        Log.e("CATEGORY_REFRESH", "Gemini future failed", e);
                    }
                }
            }
            catch (Exception e)
            {
                Log.e("CATEGORY_REFRESH", "Category update failed", e);
            }
        });
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
            Log.e("API", "Failed to fetch items", e);
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

    public boolean login(String firebaseToken)
    {
        this.firebaseToken = firebaseToken;

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
                    Log.e("LOGIN", "Login failed: " + response.code() + " " + response.message());
                    return false;
                }

                return true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
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

    public Map<String, Map<String, Map<String, Double>>> previewItems(String store, String branch)
            throws IOException, JSONException
    {
        String endpoint = "/preview_items?"
                + "store=" + URLEncoder.encode(store, "UTF-8")
                + "&branch=" + URLEncoder.encode(branch, "UTF-8");

        String raw = getRaw(endpoint);
        JSONObject itemsJson = new JSONObject(raw);

        Map<String, Map<String, Map<String, Double>>> items = new HashMap<>();

        for (Iterator<String> itemIter = itemsJson.keys(); itemIter.hasNext();)
        {
            String itemCode = itemIter.next();
            JSONObject storesJson = itemsJson.getJSONObject(itemCode);
            Map<String, Map<String, Double>> storeMap = parsePriceResponse(storesJson.toString());

            items.put(itemCode, storeMap);
        }

        return items;
    }

    public ArrayList<String> getAllBranches(String store) throws IOException, JSONException
    {
        String endpoint = "/all_branches?"
                + "store=" + URLEncoder.encode(store, "UTF-8");

        String raw = getRaw(endpoint);
        JSONArray arr = new JSONArray(raw);

        ArrayList<String> branches = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++)
        {
            branches.add(arr.getString(i));
        }

        return branches;
    }

    public Map<String, ArrayList<String>> getAllBranchesBulk(ArrayList<String> cities)
            throws IOException, JSONException
    {
        StringBuilder endpoint = new StringBuilder("/all_branches_bulk");

        if (cities != null && !cities.isEmpty())
        {
            endpoint.append("?");
            for (int i = 0; i < cities.size(); i++)
            {
                endpoint.append("cities=");
                endpoint.append(URLEncoder.encode(cities.get(i), "UTF-8"));

                if (i < cities.size() - 1)
                {
                    endpoint.append("&");
                }
            }
        }

        String raw = getRaw(endpoint.toString());
        JSONObject json = new JSONObject(raw);

        Map<String, ArrayList<String>> result = new HashMap<>();

        Iterator<String> keys = json.keys();
        while (keys.hasNext())
        {
            String store = keys.next();
            JSONArray arr = json.getJSONArray(store);

            ArrayList<String> branches = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++)
            {
                branches.add(arr.getString(i));
            }

            result.put(store, branches);
        }

        return result;
    }

    public Map<String, ArrayList<String>> getAllBranchesBulk()
            throws IOException, JSONException
    {
        return getAllBranchesBulk(null);
    }
}