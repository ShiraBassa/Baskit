package com.example.baskit.online_components;

import android.os.Build;
import android.util.Log;

import com.example.baskit.Baskit;
import com.example.baskit.main_components.Item;
import com.example.baskit.main_components.Item.ItemInfo;
import com.example.baskit.main_components.Item.ItemVariant;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.sqlite.AppDatabase;
import com.example.baskit.sqlite.GroupsEntity;
import com.example.baskit.sqlite.ItemInfoEntity;
import com.example.baskit.sqlite.ItemPricesEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("ALL")
public class APIHandler
{
    private Map<String, Map<String, Map<String, Double>>> cachedItemPrices = null;
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
            ResponseBody responseBody = response.body();

            if (!response.isSuccessful())
            {
                Log.e("API", "GET failed: " + endpoint + " -> " + response.code());
                return null; // instead of crashing, return null
            }

            if (responseBody == null)
            {
                Log.e("API", "Empty response body: " + endpoint);
                return null;
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

    @SuppressWarnings("CallToPrintStackTrace")
    public boolean login(String firebaseToken)
    {
        APIHandler.firebaseToken = firebaseToken;

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
        cachedGroups = loadGroupsFromDB();
        cachedItemInfos = loadItemInfosFromDB();

        if (cachedItemPrices.isEmpty() || cachedGroups.isEmpty() || cachedItemInfos.isEmpty())
        {
            try
            {
                updateAllCache();
            }
            catch (Exception e)
            {
                Log.e("API Preload", Objects.requireNonNull(e.getMessage()));
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
                        Log.e("API Background Refresh", Objects.requireNonNull(e.getMessage()));
                    }
                });
            }
            catch (Exception e)
            {
                Log.e("API load new data", Objects.requireNonNull(e.getMessage()));
            }
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

    private Map<String, ArrayList<String>> loadGroupsFromDB()
    {
        List<GroupsEntity> dbGroups =
                AppDatabase.getDatabase(Baskit.getContext())
                        .groupDao()
                        .getAll();

        Map<String, ArrayList<String>> groups = new HashMap<>();

        for (GroupsEntity group : dbGroups)
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
            ItemInfo info = new ItemInfo(
                    entity.itemCode,
                    entity.baseName,
                    entity.company,
                    entity.weight,
                    entity.unit,
                    entity.category
            );

            result.put(entity.itemCode, info);
        }

        return result;
    }

    private void updateAllCache()
    {
        try
        {
            var pricesFuture = networkExecutor.submit(this::getItemPricesFromAPI);
            var groupsFuture = networkExecutor.submit(this::getGroupsFromAPI);
            var infosFuture = networkExecutor.submit(this::getItemInfosFromAPI);

            Map<String, Map<String, Map<String, Double>>> freshItemPrices = pricesFuture.get();
            Map<String, ArrayList<String>> freshGroups = groupsFuture.get();
            Map<String, ItemInfo> freshItemInfos = infosFuture.get();

            if (freshItemPrices != null && !freshItemPrices.isEmpty())
            {
                cachedItemPrices = new HashMap<>(freshItemPrices);
                saveItemPricesToDB(freshItemPrices);
            }

            if (freshGroups != null && !freshGroups.isEmpty())
            {
                cachedGroups = new HashMap<>(freshGroups);
                saveGroupsToDB(freshGroups);
            }

            if (freshItemInfos != null && !freshItemInfos.isEmpty())
            {
                cachedItemInfos = new HashMap<>(freshItemInfos);
                saveItemInfosToDB(freshItemInfos);
            }
        }
        catch (Exception e)
        {
            Log.e("API", "Cache update failed but app continues", e);
        }
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

                for (String store : Objects.requireNonNull(freshStores).keySet())
                {
                    Map<String, Double> freshBranches = freshStores.get(store);

                    for (String branch : Objects.requireNonNull(freshBranches).keySet())
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

    private void saveGroupsToDB(Map<String, ArrayList<String>> groups)
    {
        dbExecutor.execute(() ->
        {
            AppDatabase db = AppDatabase.getDatabase(Baskit.getContext());
            db.groupDao().clearAll();

            List<GroupsEntity> list = new ArrayList<>();

            for (Map.Entry<String, ArrayList<String>> entry : groups.entrySet())
            {
                JSONArray arr = new JSONArray(entry.getValue());

                list.add(new GroupsEntity(
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
                entity.category = info.getCategory();

                list.add(entity);
            }

            if (!list.isEmpty())
            {
                db.itemInfoDao().insertAll(list);
            }
        });
    }

    public void updateSupermarkets() throws JSONException, IOException
    {
        Map<String, ArrayList<String>> branches = getChoices();
        this.supermarkets = Supermarket.getSupermarketsFromStrings(branches);
    }

    public ArrayList<Supermarket> getUpdatedSupermarkets() throws JSONException, IOException
    {
        updateSupermarkets();
        return getSupermarkets();
    }

    private Map<String, Map<String, Map<String, Double>>> getItemPricesFromAPI()
    {
        Map<String, Map<String, Map<String, Double>>> items = new HashMap<>();

        try
        {
            String itemsRaw = getRaw("/items_prices");
            if (itemsRaw == null) return new HashMap<>();
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

    private Map<String, ArrayList<String>> getGroupsFromAPI()
    {
        Map<String, ArrayList<String>> groups = new HashMap<>();

        try
        {
            String raw = getRaw("/groups");
            if (raw == null) return new HashMap<>();
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
            if (raw == null) return new HashMap<>();
            JSONObject json = new JSONObject(raw);

            for (Iterator<String> it = json.keys(); it.hasNext();)
            {
                String itemCode = it.next();
                JSONObject obj = json.getJSONObject(itemCode);

                String baseName = obj.optString("name", "");
                String company = obj.optString("company", "");
                String unit = obj.optString("unit", "");
                String category = obj.optString("category", "");

                double weight = 0;

                if (obj.has("weight"))
                {
                    try
                    {
                        weight = obj.getDouble("weight");
                    }
                    catch (Exception ignored) {}
                }

                ItemInfo info = new ItemInfo(
                        itemCode,
                        baseName,
                        company,
                        weight,
                        unit,
                        category
                );

                infos.put(itemCode, info);
            }
        }
        catch (Exception e)
        {
            Log.e("API", "Failed to fetch item infos", e);
        }

        return infos;
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

    public ArrayList<String> getStores() throws IOException, JSONException
    {
        String storesRaw = getRaw("/stores");
        JSONArray storesJson = new JSONArray(storesRaw);
        ArrayList<String> stores = new ArrayList<>();

        for (int i = 0; i < storesJson.length(); i++)
        {
            stores.add(storesJson.getString(i));
        }

        return stores;
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

    public Map<String, ArrayList<String>> getAllBranches(ArrayList<String> cities)
            throws IOException, JSONException
    {
        StringBuilder endpoint = new StringBuilder("/all_branches");

        if (cities != null && !cities.isEmpty())
        {
            endpoint.append("?");
            for (int i = 0; i < cities.size(); i++)
            {
                endpoint.append("cities=");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                {
                    endpoint.append(URLEncoder.encode(cities.get(i), StandardCharsets.UTF_8));
                }

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

    public ItemInfo getItemInfo(String itemCode)
    {
        if (itemCode == null || itemCode.isEmpty() || cachedItemInfos == null || cachedItemInfos.isEmpty()) return null;
        return cachedItemInfos.get(itemCode);
    }

    public Map<String, ItemInfo> getItemInfos()
    {
        return cachedItemInfos;
    }

    public ArrayList<String> getGroup(String base)
    {
        if (base == null || base.isEmpty() || cachedGroups == null || cachedGroups.isEmpty()) return new ArrayList<>();

        return cachedGroups.get(base);
    }

    public Map<String, ArrayList<String>> getGroups()
    {
        return cachedGroups;
    }

    private String getItemCategoryByName(String name)
    {
        if (name == null || name.isEmpty() || cachedGroups == null || cachedGroups.isEmpty())
        {
            return Baskit.UNKNOWN_CATEGORY;
        }

        ArrayList<String> group = cachedGroups.get(name);

        if (group == null || group.isEmpty())
        {
            return Baskit.UNKNOWN_CATEGORY;
        }

        for (String code : group)
        {
            ItemInfo info = cachedItemInfos.get(code);

            if (info == null)
            {
                continue;
            }

            String category = info.getCategory();

            if (category != null && !category.isEmpty())
            {
                return category;
            }
        }

        return Baskit.UNKNOWN_CATEGORY;
    }

    public ArrayList<Supermarket> getSupermarkets()
    {
        return supermarkets;
    }

    public String getItemCategory(Item item) throws IOException, JSONException
    {
        if (item == null) return Baskit.UNKNOWN_CATEGORY;

        String itemCode = item.getAbsoluteId();
        String itemName = item.getDecodedName();
        if ((itemCode == null  || itemCode.isEmpty()) &&
                (itemName == null || itemName.isEmpty()))
        {
            return Baskit.UNKNOWN_CATEGORY;
        }

        String endpoint = "", itemCategory;

        if (itemCode != null && !itemCode.isEmpty())
        {
            ItemInfo cachedInfo = cachedItemInfos.get(itemCode);

            if (cachedInfo != null)
            {
                itemCategory = cachedInfo.getCategory();

                if (itemCategory != null && !itemCategory.isEmpty())
                {
                    return itemCategory;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                endpoint = "/item_category?"
                        + "item_code=" + URLEncoder.encode(itemCode, StandardCharsets.UTF_8);
            }
        }
        else
        {
            itemCategory = getItemCategoryByName(itemName);

            if (itemCategory != null && !itemCategory.isEmpty())
            {
                return itemCategory;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                endpoint = "/item_category?"
                        + "item_name=" + URLEncoder.encode(itemName, StandardCharsets.UTF_8);
            }
        }

        // If not in cache -> try the server
        String raw = getRaw(endpoint);

        if (raw == null || raw.isEmpty())
        {
            return Baskit.UNKNOWN_CATEGORY;
        }

        JSONObject obj = new JSONObject(raw);
        itemCategory = obj.optString("category", Baskit.UNKNOWN_CATEGORY);

        if (itemCategory == null || itemCategory.isBlank())
        {
            itemCategory = Baskit.UNKNOWN_CATEGORY;
        }

        // Update embedded item info cache
        ItemInfo info = cachedItemInfos.get(itemCode);

        if (info != null)
        {
            info.setCategory(itemCategory);
        }

        String finalItemCategory = itemCategory;

        return itemCategory;
    }

    public Map<String, Map<String, Double>> getItemPricesByCode(String itemCode) throws IOException, JSONException
    {
        Map<String, Map<String, Double>> prices = cachedItemPrices.get(itemCode);

        if (prices != null && !prices.isEmpty())
        {
            return prices;
        }

        String endpoint = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            endpoint = "/item_prices?"
                    + "item_code=" + URLEncoder.encode(itemCode, StandardCharsets.UTF_8);
        }

        return parsePriceResponse(getRaw(endpoint));
    }

    public void setCities(ArrayList<String> cities) throws IOException, JSONException
    {
        JSONArray jsonArray = new JSONArray(cities);  // convert ArrayList<String> to JSONArray
        JSONObject body = new JSONObject().put("cities", jsonArray);

        postRaw("/cities", body.toString());
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

    public ArrayList<ItemVariant> buildVariant(Item item)
    {
        ArrayList<ItemVariant> variants = new ArrayList<>();

        if (item == null)
        {
            return variants;
        }

        String itemName = item.getBaseName();
        if (itemName == null || itemName.isEmpty()) return variants;

        ArrayList<String> groupCodes = cachedGroups.get(itemName);

        if (groupCodes == null || groupCodes.isEmpty()) return variants;

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

                    variants.add(
                            new ItemVariant(
                                    sm,
                                    price,
                                    cachedItemInfos.get(code)

                            )
                    );
                }
            }
        }

        return variants;
    }

    public Map<String, ArrayList<ItemVariant>> buildVariants(ArrayList<Item> items)
    {
        Map<String, ArrayList<ItemVariant>> variants = new HashMap<>();

        if (items == null)
        {
            return variants;
        }

        for (Item item : items)
        {
            String itemName = item.getBaseName();
            if (itemName == null || itemName.isEmpty()) continue;

            ArrayList<ItemVariant> itemVariants = buildVariant(item);
            variants.put(itemName, itemVariants);
        }

        return variants;
    }
}