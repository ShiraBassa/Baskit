package com.example.baskit.API;

import android.util.Log;

import com.example.baskit.Baskit;
import com.example.baskit.Categories.ItemViewPricesAdapter;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.ItemInfo;
import com.example.baskit.MainComponents.PriceRow;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.SQLite.AppDatabase;
import com.example.baskit.SQLite.ItemCategory;
import com.example.baskit.SQLite.ItemInfoEntity;
import com.example.baskit.SQLite.ItemPricesEntity;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class APIHandler
{
    private Map<String, Map<String, Map<String, Double>>> cachedItemPrices = null;
    private Map<String, String> cachedItemCategories = new HashMap<>();
    private Map<String, ArrayList<String>> cachedGroups = new HashMap<>();
    private Map<String, ItemInfo> cachedItemInfos = new HashMap<>();

    private ArrayList<Supermarket> supermarkets = null;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .build();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(3);

    private static String firebaseToken;
    private static APIHandler instance;

    private APIHandler() {}

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

    public boolean isServerActive()
    {
        try
        {
            Request request = new Request.Builder()
                    .url(Baskit.SERVER_URL + "/health")
                    .get()
                    .build();

            OkHttpClient timeoutClient = client.newBuilder()
                    .callTimeout(5, TimeUnit.SECONDS)
                    .build();

            try (Response response = timeoutClient.newCall(request).execute())
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
        cachedItemPrices = null;
        cachedItemCategories = null;
        cachedGroups = null;
        cachedItemInfos = null;

        preload();
    }

    private String getRaw(String endpoint) throws IOException
    {
        Request request = new Request.Builder()
                .url(Baskit.SERVER_URL + endpoint)
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

    private void postRaw(String endpoint, String body) throws IOException
    {
        Request request = new Request.Builder()
                .url(Baskit.SERVER_URL + endpoint)
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

    public boolean login(String firebaseToken)
    {
        this.firebaseToken = firebaseToken;

        try
        {
            JSONObject body = new JSONObject();
            Request request = new Request.Builder()
                    .url(Baskit.SERVER_URL + "/user")
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

    public void preload() throws JSONException, IOException
    {
        supermarkets = getUpdatedSupermarkets();

        cachedItemPrices = loadItemPricesFromDB();
        cachedItemCategories = loadCategoriesFromDB();
        cachedGroups = loadGroupsFromDB();
        cachedItemInfos = loadItemInfosFromDB();

        if (cachedItemPrices.isEmpty() || cachedItemCategories.isEmpty() || cachedGroups.isEmpty() || cachedItemInfos.isEmpty())
        {
            try
            {
                updateAllCache();
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
                        updateAllCache();
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

    private void updateAllCache() throws JSONException, IOException
    {
        try
        {
            var pricesFuture = networkExecutor.submit(this::getItemPricesFromAPI);
            var categoriesFuture = networkExecutor.submit(this::getCategoriesFromAPI);
            var groupsFuture = networkExecutor.submit(this::getGroupsFromAPI);
            var infosFuture = networkExecutor.submit(this::getItemInfosFromAPI);

            Map<String, Map<String, Map<String, Double>>> freshItemPrices = pricesFuture.get();
            Map<String, String> freshCategories = categoriesFuture.get();
            Map<String, ArrayList<String>> freshGroups = groupsFuture.get();
            Map<String, ItemInfo> freshItemInfos = infosFuture.get();

            cachedItemPrices = new HashMap<>(freshItemPrices);
            cachedItemCategories = new HashMap<>(freshCategories);
            cachedGroups = new HashMap<>(freshGroups);
            cachedItemInfos = new HashMap<>(freshItemInfos);

            saveItemPricesToDB(freshItemPrices);
            saveCategoriesToDB(freshCategories);
            saveGroupsToDB(freshGroups);
            saveItemInfosToDB(freshItemInfos);
        }
        catch (Exception e)
        {
            throw new IOException("Cache update failed", e);
        }
    }

    private Map<String, Map<String, Map<String, Double>>> loadItemPricesFromDB()
    {
        List<ItemPricesEntity> dbItems = AppDatabase.getDatabase(Baskit.getContext())
                .itemPricesDao()
                .getAll();

        Map<String, Map<String, Map<String, Double>>> result = new HashMap<>();

        for (ItemPricesEntity entity : dbItems)
        {
            result.computeIfAbsent(entity.itemCode, k -> new HashMap<>())
                    .computeIfAbsent(entity.store, k -> new HashMap<>())
                    .put(entity.branch, entity.price);
        }

        return result;
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

    private Map<String, ArrayList<String>> loadGroupsFromDB()
    {
        List<com.example.baskit.SQLite.GroupsEntity> dbGroups =
                AppDatabase.getDatabase(Baskit.getContext())
                        .groupDao()
                        .getAll();

        Map<String, ArrayList<String>> groups = new HashMap<>();

        for (com.example.baskit.SQLite.GroupsEntity group : dbGroups)
        {
            ArrayList<String> codes = new ArrayList<>();
            try
            {
                JSONArray arr = new JSONArray(group.getStructureJson());
                for (int i = 0; i < arr.length(); i++)
                {
                    codes.add(arr.getString(i));
                }
            }
            catch (Exception ignored) {}

            groups.put(group.getBaseName(), codes);
        }

        return groups;
    }

    private Map<String, ItemInfo> loadItemInfosFromDB()
    {
        List<ItemInfoEntity> dbInfos = AppDatabase.getDatabase(Baskit.getContext())
                .itemInfoDao()
                .getAll();

        Map<String, ItemInfo> result = new HashMap<>();

        for (ItemInfoEntity entity : dbInfos)
        {
            result.put(
                    entity.itemCode,
                    new ItemInfo(entity.itemCode, entity.baseName, entity.company, entity.weight, entity.unit)
            );
        }

        return result;
    }

    private void saveItemPricesToDB(Map<String, Map<String, Map<String, Double>>> freshItems)
    {
        dbExecutor.execute(() ->
        {
            AppDatabase db = AppDatabase.getDatabase(Baskit.getContext());
            db.itemPricesDao().clearAll();

            List<ItemPricesEntity> itemsToInsert = new ArrayList<>();

            for (String itemCode : freshItems.keySet())
            {
                Map<String, Map<String, Double>> freshStores = freshItems.get(itemCode);

                for (String store : freshStores.keySet())
                {
                    Map<String, Double> freshBranches = freshStores.get(store);

                    for (String branch : freshBranches.keySet())
                    {
                        double freshPrice = freshBranches.get(branch);
                        ItemPricesEntity entity = new ItemPricesEntity();
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
                db.itemPricesDao().insertAll(itemsToInsert);
            }
        });
    }

    private void saveCategoriesToDB(Map<String, String> categories)
    {
        dbExecutor.execute(() ->
        {
            AppDatabase db = AppDatabase.getDatabase(Baskit.getContext());
            db.itemCategoryDao().clearAll();

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

    private void saveGroupsToDB(Map<String, ArrayList<String>> groups)
    {
        dbExecutor.execute(() ->
        {
            AppDatabase db = AppDatabase.getDatabase(Baskit.getContext());
            db.groupDao().clearAll();

            List<com.example.baskit.SQLite.GroupsEntity> list = new ArrayList<>();

            for (Map.Entry<String, ArrayList<String>> entry : groups.entrySet())
            {
                JSONArray arr = new JSONArray(entry.getValue());

                list.add(new com.example.baskit.SQLite.GroupsEntity(
                        entry.getKey(),
                        arr.toString()
                ));
            }

            if (!list.isEmpty())
            {
                db.groupDao().insertAll(list);
            }
        });
    }

    private void saveItemInfosToDB(Map<String, ItemInfo> infos)
    {
        dbExecutor.execute(() ->
        {
            AppDatabase db = AppDatabase.getDatabase(Baskit.getContext());
            db.itemInfoDao().clearAll();

            List<ItemInfoEntity> list = new ArrayList<>();

            for (Map.Entry<String, ItemInfo> entry : infos.entrySet())
            {
                ItemInfo info = entry.getValue();

                ItemInfoEntity entity = new ItemInfoEntity();
                entity.itemCode = info.getCode();
                entity.baseName = info.getBaseName();
                entity.company = info.getCompany();
                entity.weight = info.getWeight();
                entity.unit = info.getUnit();

                list.add(entity);
            }

            if (!list.isEmpty())
            {
                db.itemInfoDao().insertAll(list);
            }
        });
    }

    private Map<String, Map<String, Map<String, Double>>> getItemPricesFromAPI()
    {
        Map<String, Map<String, Map<String, Double>>> items = new HashMap<>();

        try
        {
            String itemsRaw = getRaw("/items_prices");
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

    public Map<String, String> getCategoriesFromAPI() throws IOException, JSONException
    {
        String raw = getRaw("/categories");

        JSONObject categoriesJson = new JSONObject(raw);
        Map<String, String> categories = new HashMap<>();

        Iterator<String> keys = categoriesJson.keys();

        while (keys.hasNext())
        {
            String code = keys.next();
            String category = categoriesJson.getString(code);
            categories.put(code, category);
        }

        return categories;
    }

    private Map<String, ArrayList<String>> getGroupsFromAPI()
    {
        Map<String, ArrayList<String>> groups = new HashMap<>();

        try
        {
            String raw = getRaw("/groups");
            JSONObject json = new JSONObject(raw);

            for (Iterator<String> it = json.keys(); it.hasNext();)
            {
                String base = it.next();
                JSONArray arr = json.getJSONArray(base);

                ArrayList<String> codes = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++)
                {
                    codes.add(arr.getString(i));
                }

                groups.put(base, codes);
            }
        }
        catch (Exception e)
        {
            Log.e("API", "Failed to fetch groups", e);
        }

        return groups;
    }

    private Map<String, ItemInfo> getItemInfosFromAPI()
    {
        Map<String, ItemInfo> infos = new HashMap<>();

        try
        {
            String raw = getRaw("/item_infos");
            JSONObject json = new JSONObject(raw);

            for (Iterator<String> it = json.keys(); it.hasNext();)
            {
                String itemCode = it.next();
                JSONObject obj = json.getJSONObject(itemCode);

                String baseName = obj.optString("name", "");
                String company = obj.optString("company", "");
                String unit = obj.optString("unit", "");

                double weight = 0;

                if (obj.has("weight"))
                {
                    try
                    {
                        weight = obj.getDouble("weight");
                    }
                    catch (Exception ignored) {}
                }

                infos.put(itemCode, new ItemInfo(itemCode, baseName, company, weight, unit));
            }
        }
        catch (Exception e)
        {
            Log.e("API", "Failed to fetch item infos", e);
        }

        return infos;
    }

    public Map<String, Map<String, Map<String, Double>>> getItemPrices()
    {
        return cachedItemPrices;
    }

    public Map<String, String> getCategories()
    {
        return cachedItemCategories;
    }

    public Map<String, ArrayList<String>> getGroups()
    {
        return cachedGroups;
    }

    public Map<String, ItemInfo> getItemInfos()
    {
        return cachedItemInfos;
    }

    public String getItemCategory(Item item) throws IOException, JSONException
    {
        if (item == null) return null;

        String itemCode = item.getAbsoluteId();
        String itemName = item.getDecodedName();
        if ((itemCode == null  || itemCode.isEmpty()) &&
                (itemName == null || itemName.isEmpty())) return null;

        String endpoint, itemCategory;

        if (itemCode != null && !itemCode.isEmpty())
        {
            itemCategory = cachedItemCategories.get(itemCode); // Try cache first

            if (itemCategory != null && !itemCategory.isEmpty())
            {
                return itemCategory;
            }

            endpoint = "/item_category?"
                    + "item_code=" + URLEncoder.encode(itemCode, "UTF-8");
        }
        else
        {
            itemCategory = getItemCategoryByName(itemName);

            if (itemCategory != null && !itemCategory.isEmpty())
            {
                return itemCategory;
            }

            endpoint = "/item_category?"
                    + "item_name=" + URLEncoder.encode(itemName, "UTF-8");
        }

        // If not in cache -> try the server
        String raw = getRaw(endpoint);
        JSONObject obj = new JSONObject(raw);
        itemCategory = obj.optString("category", null);

        if (itemCategory == null || itemCategory.isEmpty())
        {
            return null;
        }

        // Update the cache
        cachedItemCategories.put(itemCode, itemCategory);

        String finalItemCategory = itemCategory;
        dbExecutor.execute(() ->
        {
            AppDatabase db = AppDatabase.getDatabase(Baskit.getContext());
            ItemCategory entity = new ItemCategory(itemCode, finalItemCategory);
            db.itemCategoryDao().insert(entity);
        });

        return itemCategory;
    }

    private String getItemCategoryByName(String name)
    {
        if (name == null || name.isEmpty() || cachedGroups == null || cachedGroups.isEmpty())
        {
            return null;
        }

        ArrayList<String> group = cachedGroups.get(name);

        if (group == null || group.isEmpty())
        {
            return null;
        }

        for (String code : group)
        {
            String category = cachedItemCategories.get(code);

            if (category != null && !category.isEmpty())
            {
                return category;
            }
        }

        return null;
    }

    public boolean hasCategory(String itemCode)
    {
        if (itemCode == null) return false;
        return cachedItemCategories.containsKey(itemCode.trim().toLowerCase());
    }

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

    private Map<String, ArrayList<String>> getChoices() throws IOException, JSONException
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
        Map<String, ArrayList<String>> branches = getChoices();
        this.supermarkets = Supermarket.getSupermarketsFromStrings(branches);
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

    public Map<String, Map<String, Double>> getItemPricesByCode(String itemCode) throws IOException, JSONException
    {
        Map<String, Map<String, Double>> prices = cachedItemPrices.get(itemCode);

        if (prices != null && !prices.isEmpty())
        {
            return prices;
        }

        String endpoint = "/item_prices?"
                + "item_code=" + URLEncoder.encode(itemCode, "UTF-8");

        return parsePriceResponse(getRaw(endpoint));
    }

    public ItemInfo getItemInfoFromAPI(String itemCode) throws IOException, JSONException
    {
        if (itemCode == null || itemCode.isEmpty()) return null;

        String endpoint = "/item_info?"
                + "item_code=" + URLEncoder.encode(itemCode, "UTF-8");

        String raw = getRaw(endpoint);
        JSONObject json = new JSONObject(raw);

        String baseName = json.optString("name", "");
        String company = json.optString("company", "");
        String unit = json.optString("unit", "");

        double weight = 0;

        if (json.has("weight"))
        {
            try
            {
                weight = json.getDouble("weight");
            }
            catch (Exception ignored) {}
        }

        return new ItemInfo(itemCode, baseName, company, weight, unit);
    }

    public ItemInfo getItemInfo(String itemCode)
    {
        if (itemCode == null || itemCode.isEmpty() || cachedItemInfos == null || cachedItemInfos.isEmpty()) return null;
        return cachedItemInfos.get(itemCode);
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

    public ArrayList<String> getGroupFromAPI(String base) throws IOException, JSONException
    {
        if (base == null || base.isEmpty()) return new ArrayList<>();

        ArrayList<String> cached = cachedGroups != null ? cachedGroups.get(base) : null;
        if (cached != null)
        {
            return cached;
        }

        String endpoint = "/group?"
                + "base=" + URLEncoder.encode(base, "UTF-8");

        String raw = getRaw(endpoint);
        JSONArray arr = new JSONArray(raw);

        ArrayList<String> codes = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++)
        {
            codes.add(arr.getString(i));
        }

        // Update cache
        if (cachedGroups != null)
        {
            cachedGroups.put(base, codes);
        }

        return codes;
    }

    public ArrayList<String> getGroup(String base) throws IOException, JSONException
    {
        if (base == null || base.isEmpty() || cachedGroups == null || cachedGroups.isEmpty()) return new ArrayList<>();

        ArrayList<String> cached = cachedGroups.get(base);
        return cached;
    }

    public String getItemGroupName(String id) throws IOException, JSONException
    {
        if (id == null || id.isEmpty()) return null;

        for (String baseName : cachedGroups.keySet())
        {
            ArrayList<String> itemCodes = cachedGroups.get(baseName);
            if (itemCodes == null || itemCodes.isEmpty()) continue;

            if (itemCodes.contains(id))
            {
                return baseName;
            }
        }

        return null;
    }

    public ArrayList<String> getItemVariants(String id) throws IOException, JSONException
    {
        if (id == null || id.isEmpty()) return new ArrayList<>();
        return cachedGroups.get(getItemGroupName(id));
    }

    public Map<String, ArrayList<PriceRow>> buildRows(ArrayList<Item> items)
    {
        Map<String, ArrayList<PriceRow>> rows = new java.util.HashMap<>();

        if (items == null)
        {
            return rows;
        }

        for (Item item : items)
        {
            String itemName = item.getBaseName();
            if (itemName == null || itemName.isEmpty()) continue;

            ArrayList<PriceRow> itemRows = buildRow(item);
            rows.put(itemName, itemRows);
        }

        return rows;
    }

    public ArrayList<PriceRow> buildRow(Item item)
    {
        ArrayList<PriceRow> rows = new ArrayList<>();

        if (item == null)
        {
            return rows;
        }

        String itemName = item.getBaseName();
        if (itemName == null || itemName.isEmpty()) return rows;

        ArrayList<String> groupCodes = cachedGroups.get(itemName);

        if (groupCodes == null || groupCodes.isEmpty()) return rows;

        for (String code : groupCodes)
        {
            Map<String, Map<String, Double>> supermarkets = cachedItemPrices.get(code);
            if (supermarkets == null) continue;

            for (Map.Entry<String, Map<String, Double>> smEntry : supermarkets.entrySet())
            {
                String supermarketName = smEntry.getKey();
                Map<String, Double> sections = smEntry.getValue();

                if (supermarketName == null || sections == null) continue;

                for (Map.Entry<String, Double> sectionEntry : sections.entrySet())
                {
                    String section = sectionEntry.getKey();
                    Double price = sectionEntry.getValue();

                    if (section == null || price == null) continue;

                    Supermarket sm = new Supermarket(supermarketName, section);

                    rows.add(
                            new PriceRow(
                                    sm,
                                    price,
                                    cachedItemInfos.get(code)

                            )
                    );
                }
            }
        }

        return rows;
    }
}