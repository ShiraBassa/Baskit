package com.example.baskit.MainComponents;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class List
{
    private String id = "";
    private String name = "";
    private ArrayList<String> userIDs = new ArrayList<>();
    private Map<String, Category> categories = new HashMap<>();
    private ArrayList<Request> requests = new ArrayList<>();

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

    public void addUser(String userID)
    {
        this.userIDs.add(userID);
    }

    public Map<String, Category> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Category> categories) {
        this.categories = categories;
    }

    public void addCategory(Category category)
    {
        this.categories.put(category.getName(), category);
    }

    public void updateCategory(Category category)
    {
        addCategory(category);
    }

    public Category getCategory(String categoryName)
    {
        return categories.get(categoryName);
    }

    public boolean hasCategory(String categoryName)
    {
        return categories.containsKey(categoryName);
    }

    public void removeCategory(String categoryName)
    {
        categories.remove(categoryName);
    }

    public ArrayList<Item> getItems()
    {
        ArrayList<Item> items = new ArrayList<>();

        for (Category category : categories.values())
        {
            items.addAll(category.getItems().values());
        }

        return items;
    }

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
    public double getTotal()
    {
        double sum = 0;

        for (Category category : categories.values())
        {
            sum += Math.round(category.getTotal() * 100.0) / 100.0;
        }

        return Math.round(sum * 100.0) / 100.0;
    }

    public ArrayList<Request> getRequests()
    {
        return requests;
    }

    public void setRequests(ArrayList<Request> requests)
    {
        this.requests = requests;
    }

    public void addRequest(Request request)
    {
        this.requests.add(request);
    }

    public void removeRequest(Request request)
    {
        this.requests.remove(request);
    }

    public boolean doesExists(Item item)
    {
        for (Category category : categories.values())
        {
            if (category.doesExists(item))
            {
                return true;
            }
        }

        return false;
    }

    public ArrayList<String> toItemNames()
    {
        ArrayList<String> itemNames = new ArrayList<>();

        for (Category category : categories.values())
        {
            itemNames.addAll(category.toItemNames());
        }

        return itemNames;
    }

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
}
