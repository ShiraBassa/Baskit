package com.example.baskit.main_components;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class User
{
    protected String id = "";
    protected String email = "";
    protected String name = "";
    protected ArrayList<String> listIDs = new ArrayList<>();

    public User() {}

    public User(String id, String email)
    {
        this.id = id != null ? id : "";
        this.email = email != null ? email : "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public ArrayList<String> getListIDs() {
        if (listIDs == null)
        {
            listIDs = new ArrayList<>();
        }
        return listIDs;
    }

    public void setListIDs(ArrayList<String> lists) {
        this.listIDs = lists != null ? lists : new ArrayList<>();
    }

    @Exclude
    public void addList(String listId)
    {
        if (listId == null)
        {
            return;
        }
        if (listIDs == null)
        {
            listIDs = new ArrayList<>();
        }
        listIDs.add(listId);
    }

    @Exclude
    public void removeList(String listId)
    {
        if (listId == null || listIDs == null)
        {
            return;
        }

        listIDs.remove(listId);
    }
}
