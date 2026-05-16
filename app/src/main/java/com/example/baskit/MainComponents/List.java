package com.example.baskit.MainComponents;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.example.baskit.MainComponents.Item.ItemVariant;

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
        this.id = id;
        this.name = name;
    }

    public List(String name)
    {
        this.name = name;
    }

    public List(List other)
    {
        if (other == null) return;

        this.id = other.getId();
        this.name = other.getName();
        this.userIDs = other.userIDs;
        this.requests = other.requests;
        this.itemSuggestions = other.itemSuggestions;

        this.categories = new HashMap<>();

        if (other.getCategories() != null)
        {
            for (Map.Entry<String, Category> entry : other.getCategories().entrySet())
            {
                Category originalCategory = entry.getValue();
                this.categories.put(entry.getKey(), new Category(originalCategory));
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getUserIDs() {
        return userIDs;
    }

    public void setUserIDs(ArrayList<String> userIDs) {
        this.userIDs = userIDs;
    }

    public Map<String, Category> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Category> categories) {
        this.categories = categories;
    }

    public ArrayList<Request> getRequests()
    {
        return requests;
    }

    public void setRequests(ArrayList<Request> requests)
    {
        this.requests = requests;
    }

    public ArrayList<String> getItemSuggestions()
    {
        return itemSuggestions;
    }

    public void setItemSuggestions(ArrayList<String> itemSuggestions)
    {
        this.itemSuggestions = itemSuggestions;
    }

    @Exclude
    public void addRequest(Request request)
    {
        this.requests.add(request);
    }

    @Exclude
    public void removeRequest(Request request)
    {
        this.requests.remove(request);
    }

    @Exclude
    public void addUser(String userID)
    {
        this.userIDs.add(userID);
    }

    @Exclude
    public void removeUser(String userID)
    {
        this.userIDs.remove(userID);
    }

    @Exclude
    public void addCategory(Category category)
    {
        this.categories.put(category.getName(), category);
    }

    @Exclude
    public void updateCategory(Category category)
    {
        removeCategory(category);
        addCategory(category);
    }

    @Exclude
    public Category getCategory(String categoryName)
    {
        return categories.get(categoryName);
    }

    @Exclude
    public boolean hasCategory(String categoryName)
    {
        return categories.containsKey(categoryName);
    }

    @Exclude
    public void removeCategory(String categoryName)
    {
        categories.remove(categoryName);
    }

    @Exclude
    public void removeCategory(Category category)
    {
        removeCategory(category.getName());
    }

    @Exclude
    public ArrayList<Item> getItems()
    {
        ArrayList<Item> items = new ArrayList<>();

        for (Category category : categories.values())
        {
            items.addAll(category.getItems());
        }

        return items;
    }

    @Exclude
    public ArrayList<Item> getRemainedItems()
    {
        ArrayList<Item> items = new ArrayList<>();

        for (Category category : categories.values())
        {
            items.addAll(category.getRemainedItems());
        }

        return items;
    }

    @Exclude
    public ArrayList<String> toItemNames()
    {
        ArrayList<String> itemNames = new ArrayList<>();

        for (Category category : categories.values())
        {
            itemNames.addAll(category.toItemNames());
        }

        return itemNames;
    }

    @Exclude
    public void removeAllItems()
    {
        this.categories = new HashMap<>();
    }

    @Exclude
    public double getTotal()
    {
        double sum = 0;

        for (Category category : categories.values())
        {
            sum += Math.round(category.getTotal() * 100.0) / 100.0;
        }

        return Math.round(sum * 100.0) / 100.0;
    }

    @Exclude
    public boolean allPricesKnown()
    {
        for (Category category : categories.values())
        {
            if (!category.allPricesKnown())
            {
                return false;
            }
        }

        return true;
    }

    @Exclude
    public boolean isEmpty()
    {
        if (categories.isEmpty())
        {
            return true;
        }

        for (Category category : categories.values())
        {
            if (!category.isEmpty())
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

        for (Item item : items)
        {
            ArrayList<ItemVariant> rows = variantsAllItems.get(item.baseName);
            if (rows == null) continue;

            item.setCheapestVariant(rows);
        }
    }

    @Exclude
    public void setSupermarketsVariants(Supermarket supermarket, Map<String, ArrayList<ItemVariant>> variantsAllItems)
    {
        if (variantsAllItems == null) return;

        ArrayList<Item> items = getItems();

        for (Item item : items)
        {
            ArrayList<ItemVariant> rows = variantsAllItems.get(item.baseName);
            if (rows == null) continue;

            item.setSupermarketVariant(supermarket, rows);
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
            this.userID = user.getId();
            this.username = user.getName();
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
