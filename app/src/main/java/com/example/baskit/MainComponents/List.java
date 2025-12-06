package com.example.baskit.MainComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    public void modifyCategory(String categoryName, Category category)
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

    public ArrayList<Item> getAllItems()
    {
        ArrayList<Item> items = new ArrayList<>();

        for (Category category : categories.values())
        {
            items.addAll(category.getItems().values());
        }

        return items;
    }

    public double getTotal()
    {
        double sum = 0;

        for (Category category : categories.values())
        {
            sum += category.getTotal();
        }

        return sum;
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
}
