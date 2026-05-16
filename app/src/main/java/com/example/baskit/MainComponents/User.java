package com.example.baskit.MainComponents;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;

public class User
{
    protected String id = "";
    protected String email = "";
    protected String name = "";
    protected ArrayList<String> listIDs = new ArrayList<>();

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

    public ArrayList<String> getListIDs() {
        return listIDs;
    }

    public void setListIDs(ArrayList<String> lists) {
        this.listIDs = lists;
    }

    @Exclude
    public void addList(String listId)
    {
        if (listIDs == null)
        {
            listIDs = new ArrayList<>();
        }

        listIDs.add(listId);
    }

    @Exclude
    public void removeList(String listId)
    {
        listIDs.remove(listId);
    }
}
