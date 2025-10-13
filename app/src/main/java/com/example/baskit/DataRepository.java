package com.example.baskit;

import com.example.baskit.MainComponents.List;

import java.util.ArrayList;

public class DataRepository {
    private static DataRepository instance;
    private ArrayList<List> lists;

    private DataRepository()
    {
        lists = new ArrayList<>();
    }

    public static DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    public ArrayList<List> getLists() {
        return lists;
    }

    public void addList(List list)
    {
        lists.add(list);
    }

    public void removeList(List list)
    {
        lists.remove(list);
    }

    public void removeList(String listId)
    {
        lists.remove(getList(listId));
    }

    public List getList(String id)
    {
        for (List list : lists) {
            if (list.getId().equals(id))
            {
                return list;
            }
        }
        return null;
    }

    public void refreshLists(ArrayList<List> lists)
    {
        this.lists = lists;
    }

    public void refreshList(List list)
    {
        int index = lists.indexOf(getList(list.getId()));

        if (index != -1)
        {
            lists.set(index, list);
        }
    }
}