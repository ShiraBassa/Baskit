package com.example.baskit.Firebase;

import static com.example.baskit.Firebase.FBRefs.refLists;
import static com.example.baskit.Firebase.FBRefs.refUsers;

import androidx.annotation.NonNull;

import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FirebaseDBHandler
{
    public interface GetListCallback
    {
        void onListFetched(List newList);
        void onError(String error);
    }

    public interface GetCategoryCallback
    {
        void onCategoryFetched(Category newCategory);
        void onError(String error);
    }

    public interface GetListNamesListenerCallback
    {
        void onInfoFetched(ArrayList<String> listNames);
    }

    public interface GetListNamesCallback
    {
        void onNamesFetched(ArrayList<String> listNames);
    }

    public interface GetUserNameCallback
    {
        void onUserNameFetched(String username);
    }

    private static FirebaseDBHandler instance;
    private ArrayList<String> lastListNames = new ArrayList<>();

    public static FirebaseDBHandler getInstance()
    {
        if (instance == null)
        {
            instance = new FirebaseDBHandler();
        }

        return instance;
    }

    public void getList(String listId, GetListCallback callback)
    {
        refLists.child(listId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful())
            {
                DataSnapshot snapshot = task.getResult();

                if (snapshot.exists())
                {
                    List list = snapshot.getValue(List.class);
                    callback.onListFetched(list);
                }
                else
                {
                    callback.onError("List not found");
                }
            }
            else
            {
                callback.onError(task.getException().getMessage());
            }
        });
    }

    public void getListNames(ArrayList<String> listIDs, GetListNamesCallback callback) {
        if (listIDs == null || listIDs.isEmpty())
        {
            callback.onNamesFetched(new ArrayList<>());
            return;
        }

        Map<String, List> fetchedListsMap = new HashMap<>();
        final int[] completed = {0};

        for (String id : listIDs)
        {
            refLists.child(id).get().addOnCompleteListener(task ->
            {
                completed[0]++;

                if (task.isSuccessful() && task.getResult().exists())
                {
                    List list = task.getResult().getValue(List.class);

                    if (list != null)
                    {
                        fetchedListsMap.put(id, list);
                    }
                }

                if (completed[0] == listIDs.size())
                {
                    ArrayList<String> finalListNames = new ArrayList<>();

                    for (String listId : listIDs)
                    {
                        List list = fetchedListsMap.get(listId);

                        if (list != null)
                        {
                            finalListNames.add(list.getName());
                        }
                    }

                    callback.onNamesFetched(finalListNames);
                }
            });
        }
    }

    public void listenToUserName(User user, GetUserNameCallback callback)
    {
        refUsers.child(user.getId()).child("name").addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if (snapshot.exists())
                {
                    String new_name = snapshot.getValue(String.class);

                    if (new_name == null)
                    {
                        return;
                    }

                    if (!new_name.equals(user.getName()))
                    {
                        callback.onUserNameFetched(new_name);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void removeList(String listId, User user)
    {
        refLists.child(listId).removeValue();

        user.removeList(listId);
        refUsers.child(user.getId()).child("listIDs").setValue(user.getListIDs());
    }

    public void addList(List list, User user)
    {
        refLists.child(list.getId()).setValue(list);

        user.addList(list.getId());
        refUsers.child(user.getId()).child("listIDs").setValue(user.getListIDs());
    }

    public DatabaseReference getListRef(String listId)
    {
        return refLists.child(listId);
    }

    public void addCategory(List list, Category category)
    {
        list.addCategory(category);
        refLists.child(list.getId()).
                child("categories").
                child(category.getName()).
                setValue(category);
    }

    public String getUniqueId()
    {
        return refLists.push().getKey();
    }

    public void addItem(List list, Category category, Item item)
    {
        category.addItem(item);
        refLists.child(list.getId())
                .child("categories")
                .child(category.getName())
                .child("items")
                .child(item.getId())
                .setValue(item);
    }

    public void addItem(List list, String category_name, Item item)
    {
        list.getCategory(category_name).addItem(item);
        refLists.child(list.getId())
                .child("categories")
                .child(category_name)
                .child("items")
                .child(item.getId())
                .setValue(item);
    }

    public void listenToList(String listId, GetListCallback callback)
    {
        refLists.child(listId).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if (snapshot.exists())
                {
                    List list = snapshot.getValue(List.class);
                    callback.onListFetched(list);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void listenToCategory(List list, Category category, GetCategoryCallback callback)
    {
        refLists.child(list.getId()).child("categories").child(category.getName()).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if (snapshot.exists())
                {
                    Category newCategory = snapshot.getValue(Category.class);

                    if (newCategory != null)
                    {
                        callback.onCategoryFetched(newCategory);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void listenToListNames(String userId, GetListNamesListenerCallback callback)
    {
        refUsers.child(userId).child("listIDs").addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                ArrayList<String> listIDs = snapshot.getValue(new GenericTypeIndicator<ArrayList<String>>() {});
                ArrayList<String> listNames;

                if (listIDs == null)
                {
                    listNames = new ArrayList<>();
                    callback.onInfoFetched(listNames);
                    return;
                }

                getListNames(listIDs, new GetListNamesCallback()
                {
                    @Override
                    public void onNamesFetched(ArrayList<String> listNames)
                    {
                        if (!listNames.equals(lastListNames))
                        {
                            lastListNames = listNames;
                            callback.onInfoFetched(listNames);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void updateItem(List list, Category category, Item item)
    {
        refLists.child(list.getId())
                .child("categories")
                .child(category.getName())
                .child("items")
                .child(item.getId())
                .setValue(item);
    }

    public void removeItem(List list, Category category, Item item)
    {
        refLists.child(list.getId())
                .child("categories")
                .child(category.getName())
                .child("items")
                .child(item.getId())
                .removeValue();
    }

    public void removeCategory(List list, Category category)
    {
        refLists.child(list.getId())
                .child("categories")
                .child(category.getName())
                .removeValue();
    }
}
