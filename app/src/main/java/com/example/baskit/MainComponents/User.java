package com.example.baskit.MainComponents;

import java.util.ArrayList;

public class User
{
    protected String id = "";
    protected String email = "";
    protected String name = "";
    private String token = "";
    protected ArrayList<String> cities = new ArrayList<>();
    protected ArrayList<String> listIDs = new ArrayList<>();
    protected ArrayList<Supermarket> supermarkets = new ArrayList<>();

    public User(String email)
    {
        this.email = email;
    }

    public User() {}

    public User(String id, String email)
    {
        this.id = id;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getCities() {
        return cities;
    }

    public void setCities(ArrayList<String> cities) {
        this.cities = cities;
    }

    public ArrayList<String> getListIDs() {
        return listIDs;
    }

    public void setListIDs(ArrayList<String> lists) {
        this.listIDs = lists;
    }

    public ArrayList<Supermarket> getSupermarkets() {
        return supermarkets;
    }

    public void setSupermarkets(ArrayList<Supermarket> supermarkets) {
        this.supermarkets = supermarkets;
    }

    public void addList(String listId)
    {
        if (listIDs == null)
        {
            listIDs = new ArrayList<>();
        }

        listIDs.add(listId);
    }

    public void removeList(String listId)
    {
        listIDs.remove(listId);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
