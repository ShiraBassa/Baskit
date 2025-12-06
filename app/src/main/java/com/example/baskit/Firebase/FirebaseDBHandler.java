package com.example.baskit.Firebase;

import static com.example.baskit.Firebase.FBRefs.refLists;
import static com.example.baskit.Firebase.FBRefs.refUsers;

import androidx.annotation.NonNull;

import com.example.baskit.MainComponents.Category;
import com.example.baskit.MainComponents.Item;
import com.example.baskit.MainComponents.List;
import com.example.baskit.MainComponents.Request;
import com.example.baskit.MainComponents.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FirebaseDBHandler
{
    private FirebaseDBHandler() {}

    public interface GetListCallback
    {
        void onListFetched(List newList);
        void onError(String error);
    }

    public interface DBCallback {
        void onComplete();
        void onFailure(Exception e);
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

    public interface GetRequestsCallback
    {
        void onRequestsFetched(ArrayList<Request> requests);
    }

    public interface GetUserCallback
    {
        void onUserFetched(User newUser);
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
                    removeList(listId);
                    callback.onError("List not found");
                }
            }
            else
            {
                callback.onError(task.getException().getMessage());
            }
        });
    }

    public void getUser(String userID, GetUserCallback callback)
    {
        refLists.child(userID).get().addOnCompleteListener(task ->
        {
            if (task.isSuccessful())
            {
                DataSnapshot snapshot = task.getResult();

                if (snapshot.exists())
                {
                    User user = snapshot.getValue(User.class);
                    callback.onUserFetched(user);
                }
            }
        });
    }

    public void getListNames(User user, GetListNamesCallback callback)
    {
        ArrayList<String> listIDs = user.getListIDs();

        if (listIDs == null || listIDs.isEmpty())
        {
            callback.onNamesFetched(new ArrayList<>());
            return;
        }

        ArrayList<String> validListIDs = new ArrayList<>();
        ArrayList<String> finalListNames = new ArrayList<>();
        final int[] completed = {0};

        for (String listID : listIDs)
        {
            refLists.child(listID).get().addOnCompleteListener(task ->
            {
                completed[0]++;

                if (task.isSuccessful() && task.getResult().exists())
                {
                    List list = task.getResult().getValue(List.class);

                    if (list != null && list.getUserIDs().contains(user.getId()))
                    {
                        validListIDs.add(listID);
                        finalListNames.add(list.getName());
                    }
                }

                if (completed[0] == listIDs.size())
                {
                    if (validListIDs.isEmpty())
                    {
                        refUsers.child(user.getId()).child("listIDs").removeValue();
                        user.setListIDs(new ArrayList<>());
                    }
                    else
                    {
                        refUsers.child(user.getId()).child("listIDs").setValue(validListIDs);
                        user.setListIDs(validListIDs);
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

    public void removeList(String listId)
    {
        refLists.child(listId).get().addOnCompleteListener(task ->
        {
            if (task.isSuccessful() && task.getResult().exists())
            {
                List list = task.getResult().getValue(List.class);

                if (list != null)
                {
                    removeList(list);
                }
            }
        });
    }

    public void removeList(List list)
    {
        ArrayList<String> userIDs = list.getUserIDs();

        for (String userID : userIDs)
        {
            refUsers.child(userID).child("listIDs").get().addOnCompleteListener(task ->
            {
                if (task.isSuccessful())
                {
                    ArrayList<String> listIDs = task.getResult().getValue(new GenericTypeIndicator<ArrayList<String>>() {});

                    if (listIDs != null && listIDs.contains(list.getId()))
                    {
                        listIDs.remove(list.getId());

                        if (listIDs.isEmpty())
                        {
                            refUsers.child(userID).child("listIDs").removeValue();
                        }
                        else
                        {
                            refUsers.child(userID).child("listIDs").setValue(listIDs);
                        }
                    }
                }
            });
        }

        refLists.child(list.getId()).removeValue();
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

        DatabaseReference categoryRef = refLists
                .child(list.getId())
                .child("categories")
                .child(category.getName());

        categoryRef.setValue(category);
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

    public void addItem(List list, String category_name, Item item, DBCallback callback)
    {
        list.getCategory(category_name).addItem(item);

        refLists.child(list.getId())
                .child("categories")
                .child(category_name)
                .child("items")
                .child(item.getId())
                .setValue(item)
                .addOnSuccessListener(aVoid ->
                {
                    if (callback != null) callback.onComplete();
                })
                .addOnFailureListener(e ->
                {
                    if (callback != null) callback.onFailure(e);
                });
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

    public void listenToListNames(User user, GetListNamesListenerCallback callback)
    {
        refUsers.child(user.getId()).child("listIDs").addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                ArrayList<String> listIDs = snapshot.getValue(new GenericTypeIndicator<ArrayList<String>>() {});
                ArrayList<String> listNames;

                user.setListIDs(listIDs);

                if (listIDs == null)
                {
                    listNames = new ArrayList<>();
                    callback.onInfoFetched(listNames);
                    return;
                }

                getListNames(user, new GetListNamesCallback()
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

    public void updateItem(List list, Item item)
    {
        updateItem(list, getCategory(list, item), item);
    }

    public Category getCategory(List list, Item item)
    {
        for (Category category : list.getCategories().values())
        {
            if (category.getItems().containsValue(item))
            {
                return category;
            }
        }

        return null;
    }

    public void updateItems(List list, ArrayList<Item> items)
    {
        if (items == null) return;

        for (Item item : items)
        {
            for (Category category : list.getCategories().values())
            {
                if (category.getItems().containsKey(item.getId()))
                {
                    updateItem(list, category, item);
                    break;
                }
            }
        }
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

    public void removeItem(List list, Item item)
    {
        removeItem(list, getCategory(list, item), item);
    }

    public void removeCategory(List list, Category category)
    {
        refLists.child(list.getId())
                .child("categories")
                .child(category.getName())
                .removeValue();
    }

    public void acceptRequest(List list, Request request)
    {
        removeRequest(list, request);

        list.addUser(request.getUserID());
        refLists.child(list.getId())
                .child("userIDs")
                .setValue(list.getUserIDs());

        refUsers.child(request.getUserID())
                .child("listIDs")
                .get().addOnCompleteListener(task ->
                {
                    if (task.isSuccessful())
                    {
                        ArrayList<String> listIDs = task.getResult().getValue(
                                new GenericTypeIndicator<ArrayList<String>>() {});

                        if (listIDs == null)
                        {
                            listIDs = new ArrayList<>();
                        }

                        if (!listIDs.contains(list.getId()))
                        {
                            listIDs.add(list.getId());
                        }

                        refUsers.child(request.getUserID())
                                .child("listIDs")
                                .setValue(listIDs);
                    }
                });
    }

    public void declineRequest(List list, Request request)
    {
        removeRequest(list, request);
    }

    private void removeRequest(List list, Request request)
    {
        list.removeRequest(request);

        refLists.child(list.getId())
                .child("requests")
                .setValue(list.getRequests());
    }

    public void addRequest(List list, Request request)
    {
        list.addRequest(request);

        refLists.child(list.getId())
                .child("requests")
                .setValue(list.getRequests());
    }

    public void getUsername(String userID, GetUserNameCallback callback)
    {
        refUsers.child(userID).child("name").get().addOnCompleteListener(task ->
        {
            if (task.isSuccessful() && task.getResult().exists())
            {
                String username = task.getResult().getValue(String.class);
                callback.onUserNameFetched(username);
            }
        });
    }

    public void listenForRequests(List list, GetRequestsCallback callback)
    {
        refLists.child(list.getId()).child("requests")
            .addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot)
                {
                    ArrayList<Request> updatedRequests = snapshot.getValue(
                            new GenericTypeIndicator<ArrayList<Request>>() {});

                    if (updatedRequests == null)
                    {
                        updatedRequests = new ArrayList<>();
                        callback.onRequestsFetched(updatedRequests);
                        return;
                    }

                    list.setRequests(updatedRequests);
                    callback.onRequestsFetched(updatedRequests);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }
}
