package com.example.baskit.main_components;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.example.baskit.main_components.Item.ItemVariant;

@SuppressWarnings("unused")
@IgnoreExtraProperties
public class List implements SortableEntity
{
    private String id = "";
    private String name = "";
    private ArrayList<String> userIDs = new ArrayList<>();
    private Map<String, Category> categories = new HashMap<>();
    private ArrayList<Request> requests = new ArrayList<>();
    ArrayList<String> itemSuggestions = new ArrayList<>();

    public List() {}

    public List(String id, String name)
    {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
    }

    public List(String name)
    {
        this.name = name != null ? name : "";
    }

    public List(List other)
    {
        if (other == null) return;

        this.id = other.getId();
        this.name = other.getName();
        this.userIDs = other.userIDs != null
                ? new ArrayList<>(other.userIDs)
                : new ArrayList<>();

        this.requests = other.requests != null
                ? new ArrayList<>(other.requests)
                : new ArrayList<>();

        this.itemSuggestions = other.itemSuggestions != null
                ? new ArrayList<>(other.itemSuggestions)
                : new ArrayList<>();

        this.categories = new HashMap<>();

        if (other.getCategories() != null)
        {
            for (Map.Entry<String, Category> entry : other.getCategories().entrySet())
            {
                if (entry == null || entry.getKey() == null)
                {
                    continue;
                }
                Category originalCategory = entry.getValue();
                this.categories.put(entry.getKey(), new Category(originalCategory));
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public ArrayList<String> getUserIDs() {
        if (userIDs == null)
        {
            userIDs = new ArrayList<>();
        }
        return userIDs;
    }

    public void setUserIDs(ArrayList<String> userIDs) {
        this.userIDs = userIDs != null ? userIDs : new ArrayList<>();
    }

    public Map<String, Category> getCategories() {
        if (categories == null)
        {
            categories = new HashMap<>();
        }
        return categories;
    }

    public void setCategories(Map<String, Category> categories) {
        this.categories = categories != null ? categories : new HashMap<>();
    }

    public ArrayList<Request> getRequests()
    {
        if (requests == null)
        {
            requests = new ArrayList<>();
        }
        return requests;
    }

    public void setRequests(ArrayList<Request> requests)
    {
        this.requests = requests != null ? requests : new ArrayList<>();
    }

    public ArrayList<String> getItemSuggestions()
    {
        if (itemSuggestions == null)
        {
            itemSuggestions = new ArrayList<>();
        }
        return itemSuggestions;
    }

    public void setItemSuggestions(ArrayList<String> itemSuggestions)
    {
        this.itemSuggestions = itemSuggestions != null ? itemSuggestions : new ArrayList<>();
    }

    @Exclude
    public void addRequest(Request request)
    {
        if (request == null)
        {
            return;
        }

        if (requests == null)
        {
            requests = new ArrayList<>();
        }
        this.requests.add(request);
    }

    @Exclude
    public void removeRequest(Request request)
    {
        if (request == null || requests == null)
        {
            return;
        }
        this.requests.remove(request);
    }

    @Exclude
    public void addUser(String userID)
    {
        if (userID == null)
        {
            return;
        }

        if (userIDs == null)
        {
            userIDs = new ArrayList<>();
        }
        this.userIDs.add(userID);
    }

    @Exclude
    public void removeUser(String userID)
    {
        if (userID == null || userIDs == null)
        {
            return;
        }
        this.userIDs.remove(userID);
    }

    @Exclude
    public void addCategory(Category category)
    {
        if (category == null || category.getName() == null)
        {
            return;
        }

        if (categories == null)
        {
            categories = new HashMap<>();
        }
        this.categories.put(category.getName(), category);
    }

    @Exclude
    public void updateCategory(Category category)
    {
        if (category == null)
        {
            return;
        }
        removeCategory(category);
        addCategory(category);
    }

    @Exclude
    public Category getCategory(String categoryName)
    {
        return categories != null && categoryName != null
                ? categories.get(categoryName)
                : null;
    }

    @Exclude
    public boolean hasCategory(String categoryName)
    {
        return categories != null && categories.containsKey(categoryName);
    }

    @Exclude
    public void removeCategory(String categoryName)
    {
        if (categoryName == null || categories == null)
        {
            return;
        }
        categories.remove(categoryName);
    }

    @Exclude
    public void removeCategory(Category category)
    {
        if (category == null)
        {
            return;
        }

        removeCategory(category.getName());
    }

    @Exclude
    public ArrayList<Item> getItems()
    {
        ArrayList<Item> items = new ArrayList<>();

        if (categories == null)
        {
            return items;
        }

        for (Category category : categories.values())
        {
            if (category != null && category.getItems() != null)
            {
                items.addAll(category.getItems());
            }
        }

        return items;
    }

    @Exclude
    public ArrayList<Item> getRemainedItems()
    {
        ArrayList<Item> items = new ArrayList<>();

        if (categories == null)
        {
            return items;
        }

        for (Category category : categories.values())
        {
            if (category != null && category.getRemainedItems() != null)
            {
                items.addAll(category.getRemainedItems());
            }
        }

        return items;
    }

    @Exclude
    public ArrayList<String> toItemNames()
    {
        ArrayList<String> itemNames = new ArrayList<>();

        if (categories == null)
        {
            return itemNames;
        }

        for (Category category : categories.values())
        {
            if (category != null && category.toItemNames() != null)
            {
                itemNames.addAll(category.toItemNames());
            }
        }

        return itemNames;
    }

    @Exclude
    public void removeAllItems()
    {
        this.categories = new HashMap<>();

        if (requests == null)
        {
            requests = new ArrayList<>();
        }
    }

    @Exclude
    public double getTotal()
    {
        if (categories == null)
        {
            return 0.0;
        }

        double sum = 0;

        for (Category category : categories.values())
        {
            if (category != null)
            {
                double categoryTotal = Math.round(category.getTotal() * 100.0) / 100.0;

                if (Double.isNaN(categoryTotal) || Double.isInfinite(categoryTotal))
                {
                    categoryTotal = 0.0;
                }

                sum += categoryTotal;
            }
        }

        return Math.round(sum * 100.0) / 100.0;
    }

    @Exclude
    public boolean allPricesKnown()
    {
        if (categories == null)
        {
            return true;
        }

        for (Category category : categories.values())
        {
            if (category != null && !category.allPricesKnown())
            {
                return false;
            }
        }

        return true;
    }

    @Exclude
    public boolean isEmpty()
    {
        if (categories == null || categories.isEmpty())
        {
            return true;
        }

        for (Category category : categories.values())
        {
            if (category != null && !category.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    @Exclude
    public void setCheapestVariants(Map<String, ArrayList<ItemVariant>> variantsAllItems)
    {
        if (variantsAllItems == null) return;
        ArrayList<Item> items = getItems();

        if (items == null)
        {
            return;
        }

        for (Item item : items)
        {
            if (item == null || item.baseName == null)
            {
                continue;
            }

            ArrayList<ItemVariant> variants = variantsAllItems.get(item.baseName);
            if (variants == null) continue;

            item.setCheapestVariant(variants);
        }
    }

    @Exclude
    public void setSupermarketsVariants(Supermarket supermarket, Map<String, ArrayList<ItemVariant>> variantsAllItems)
    {
        if (supermarket == null)
        {
            return;
        }

        if (variantsAllItems == null) return;

        ArrayList<Item> items = getItems();

        if (items == null)
        {
            return;
        }

        for (Item item : items)
        {
            if (item == null || item.baseName == null)
            {
                continue;
            }

            ArrayList<ItemVariant> variants = variantsAllItems.get(item.baseName);
            if (variants == null) continue;

            item.setSupermarketVariant(supermarket, variants);
        }
    }

    @Exclude
    @Override
    public SortableEntity copy()
    {
        return new List(this);
    }


    public static class Request
    {
        private String userID = "";
        private String username = "";

        public Request() {}

        public Request(String userID, String username)
        {
            this.userID = userID;
            this.username = username;
        }

        public Request(User user)
        {
            if (user == null)
            {
                return;
            }
            this.userID = user.getId();
            this.username = user.getName();
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID != null ? userID : "";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username != null ? username : "";
        }
    }
}
