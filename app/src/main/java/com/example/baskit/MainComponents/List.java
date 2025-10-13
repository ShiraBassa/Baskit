package com.example.baskit.MainComponents;

import java.util.ArrayList;

public class List
{
    private String id = "";
    private String name = "";
    private ArrayList<String> users;
    private ArrayList<Category> categories;

    public List() {}

    public List(String id, String name)
    {
        this.id = id;
        this.name = name;
        this.categories = new ArrayList<>();
    }

    public List(String name)
    {
        this.name = name;
        this.categories = new ArrayList<>();
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

    public ArrayList<String> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<String> users) {
        this.users = users;
    }

    public ArrayList<Category> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<Category> categories) {
        this.categories = categories;
    }

    public void addCategory(Category category)
    {
        this.categories.add(category);
    }

    public void modifyCategory(String categoryName, Category category)
    {
        categories.set(categories.indexOf(getCategory(categoryName)), category);
    }

    public Category getCategory(String categoryName)
    {
        for (Category category : categories)
        {
            if (category.getName().equals(categoryName))
            {
                return category;
            }
        }

        return null;
    }

    public boolean hasCategory(String categoryName)
    {
        return getCategory(categoryName) != null;
    }

    public void removeCategory(String categoryName)
    {
        categories.removeIf(category -> category.getName().equals(categoryName));
    }
}
