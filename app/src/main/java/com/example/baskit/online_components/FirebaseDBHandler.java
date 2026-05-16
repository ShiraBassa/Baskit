package com.example.baskit.online_components;

import static com.example.baskit.online_components.FBRefs.refLists;
import static com.example.baskit.online_components.FBRefs.refUsers;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.baskit.main_components.Category;
import com.example.baskit.main_components.Item;
import com.example.baskit.main_components.List;
import com.example.baskit.main_components.List.Request;
import com.example.baskit.main_components.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class FirebaseDBHandler
{
    private static FirebaseDBHandler instance;

    private FirebaseDBHandler() {}

    public interface GetListCallback
    {
        void onListFetched(List newList) throws JSONException, IOException;
        void onError();
    }

    public interface DBCallback
    {
        void onComplete();
        void onFailure(Exception e);
    }


    public static FirebaseDBHandler getInstance()
    {
        if (instance == null)
        {
            instance = new FirebaseDBHandler();
        }

        return instance;
    }

    public void listenToUserName(User user, Consumer<String> callback)
    {
        refUsers.child(user.getId()).child("name").addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                String new_name = null;

                if (snapshot.exists())
                {
                    new_name = snapshot.getValue(String.class);
                }

                if (new_name == null || !new_name.equals(user.getName()))
                {
                    callback.accept(new_name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void listenToListNames(User user, Consumer<ArrayList<String>> callback)
    {
        refUsers.child(user.getId()).child("listIDs").addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                ArrayList<String> listIDs;

                try
                {
                    listIDs = snapshot.getValue(new GenericTypeIndicator<>() {
                    });
                }
                catch (DatabaseException e)
                {
                    listIDs = new ArrayList<>();
                }

                ArrayList<String> listNames;

                user.setListIDs(listIDs);

                if (listIDs == null)
                {
                    listNames = new ArrayList<>();
                    callback.accept(listNames);
                    return;
                }

                // Listen to name changes for each list
                for (String listId : listIDs)
                {
                    refLists.child(listId).child("name").addValueEventListener(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot nameSnapshot)
                        {
                            getListNames(user, listNames1 -> callback.accept(new ArrayList<>(listNames1)));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void listenToList(String listId, GetListCallback callback)
    {
        refLists.child(listId).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                List list = null;

                if (snapshot.exists())
                {
                    list = parseListSnapshot(listId, snapshot);
                }

                try
                {
                    callback.onListFetched(list);
                }
                catch (JSONException | IOException e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void listenToCategory(List list, String categoryName, Consumer<Category> callback)
    {
        refLists.child(list.getId()).child("categories").child(categoryName).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if (!snapshot.exists())
                {
                    callback.accept(null);
                    return;
                }

                String catName = snapshot.getKey();
                Category newCategory = new Category(catName);

                ArrayList<Item> items = new ArrayList<>();
                DataSnapshot itemsSnap = snapshot.child("items");

                for (DataSnapshot itemSnap : itemsSnap.getChildren())
                {
                    Item item = itemSnap.getValue(Item.class);
                    if (item != null) items.add(item);
                }

                newCategory.setItems(items);
                callback.accept(newCategory);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void listenForRequests(List list, Consumer<ArrayList<Request>> callback)
    {
        refLists.child(list.getId()).child("requests")
                .addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot)
                    {
                        ArrayList<Request> updatedRequests;
                        try {
                            updatedRequests = snapshot.getValue(
                                    new GenericTypeIndicator<>() {
                                    });
                        } catch (DatabaseException e) {
                            updatedRequests = new ArrayList<>();
                        }

                        if (updatedRequests == null)
                        {
                            updatedRequests = new ArrayList<>();
                            callback.accept(updatedRequests);
                            return;
                        }

                        list.setRequests(updatedRequests);
                        callback.accept(updatedRequests);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public void changeUserName(User user, String username)
    {
        if (user.getName().equals(username))
        {
            return;
        }

        refUsers.child(user.getId()).child("name").setValue(username);
    }

    public void getListNames(User user, Consumer<ArrayList<String>> callback)
    {
        ArrayList<String> listIDs = user.getListIDs();

        if (listIDs == null || listIDs.isEmpty())
        {
            callback.accept(new ArrayList<>());
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
                    DataSnapshot snap = task.getResult();

                    String name = snap.child("name").getValue(String.class);

                    ArrayList<String> userIDs = new ArrayList<>();
                    DataSnapshot usersSnap = snap.child("userIDs");

                    for (DataSnapshot child : usersSnap.getChildren())
                    {
                        String uid = child.getValue(String.class);
                        if (uid != null) userIDs.add(uid);
                    }

                    if (name != null && userIDs.contains(user.getId()))
                    {
                        validListIDs.add(listID);
                        finalListNames.add(name);
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

                    callback.accept(finalListNames);
                }
            });
        }
    }

    public void getList(String listId, GetListCallback callback)
    {
        refLists.child(listId).get().addOnCompleteListener(task ->
        {
            if (task.isSuccessful())
            {
                DataSnapshot snapshot = task.getResult();

                if (snapshot.exists())
                {
                    List list = parseListSnapshot(listId, snapshot);
                    try {
                        callback.onListFetched(list);
                    } catch (JSONException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else
                {
                    removeList(listId);
                    callback.onError();
                }
            }
            else
            {
                callback.onError();
            }
        });
    }

    public void removeList(String listId)
    {
        refLists.child(listId).get().addOnCompleteListener(task ->
        {
            if (task.isSuccessful() && task.getResult().exists())
            {
                DataSnapshot snap = task.getResult();
                List list = new List();
                list.setId(listId);

                ArrayList<String> userIDs = new ArrayList<>();
                for (DataSnapshot child : snap.child("userIDs").getChildren())
                {
                    String uid = child.getValue(String.class);
                    if (uid != null) userIDs.add(uid);
                }
                list.setUserIDs(userIDs);

                removeList(list);
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
                    ArrayList<String> listIDs;
                    try {
                        listIDs = task.getResult().getValue(new GenericTypeIndicator<>() {
                        });
                    } catch (DatabaseException e) {
                        listIDs = new ArrayList<>();
                    }

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

    public void updateList(List list)
    {
        for (Category category : list.getCategories().values())
        {
            updateCategory(list, category);
        }
    }

    public void finishList(List list)
    {
        for (Category category : list.getCategories().values())
        {
            finishCategory(list, category);
        }
    }

    public void removeItems(String listID)
    {
        refLists.child(listID)
                .child("categories")
                .removeValue();
    }

    public void removeItems(List list)
    {
        removeItems(list.getId());
    }

    public void leaveList(List list, User user)
    {
        if (list == null || user == null) return;

        String listID = list.getId();
        String userID = user.getId();

        ArrayList<String> userIDs = list.getUserIDs();
        ArrayList<String> listIDs = user.getListIDs();

        if (userIDs != null)
        {
            userIDs.remove(userID);
        }

        if (listIDs != null)
        {
            listIDs.remove(listID);
        }

        refLists.child(listID).child("userIDs").setValue(userIDs != null ? userIDs : new ArrayList<>());
        refUsers.child(userID).child("listIDs").setValue(listIDs != null ? listIDs : new ArrayList<>());
    }

    public void renameList(String listID, String newName)
    {
        refLists.child(listID)
                .child("name")
                .setValue(newName);
    }

    public void renameList(List list, String newName)
    {
        renameList(list.getId(), newName);
    }

    private List parseListSnapshot(String listId, DataSnapshot snapshot)
    {
        List list = new List();
        list.setId(listId);
        list.setName(snapshot.child("name").getValue(String.class));

        // userIDs
        ArrayList<String> userIDs = new ArrayList<>();
        for (DataSnapshot child : snapshot.child("userIDs").getChildren())
        {
            String uid = child.getValue(String.class);
            if (uid != null) userIDs.add(uid);
        }
        list.setUserIDs(userIDs);

        // requests
        ArrayList<Request> requests = new ArrayList<>();
        for (DataSnapshot reqSnap : snapshot.child("requests").getChildren())
        {
            Request req = reqSnap.getValue(Request.class);
            if (req != null) requests.add(req);
        }
        list.setRequests(requests);

        // itemSuggestions
        ArrayList<String> suggestions = new ArrayList<>();
        for (DataSnapshot sugSnap : snapshot.child("itemSuggestions").getChildren())
        {
            String s = sugSnap.getValue(String.class);
            if (s != null) suggestions.add(s);
        }
        list.setItemSuggestions(suggestions);

        // categories
        for (DataSnapshot catSnap : snapshot.child("categories").getChildren())
        {
            String catName = catSnap.getKey();
            Category cat = new Category(catName);

            Boolean finished = catSnap.child("finished").getValue(Boolean.class);
            if (finished != null) {
                cat.setFinished(finished);
            }

            ArrayList<Item> items = new ArrayList<>();
            DataSnapshot itemsSnap = catSnap.child("items");

            for (DataSnapshot itemSnap : itemsSnap.getChildren())
            {
                Item item = itemSnap.getValue(Item.class);
                if (item != null)
                {
                    items.add(item);
                }
            }

            cat.setItems(items);
            list.addCategory(cat);
        }

        return list;
    }

    public String getUniqueId()
    {
        return refLists.push().getKey();
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

    public void removeCategory(List list, Category category)
    {
        refLists.child(list.getId())
                .child("categories")
                .child(category.getName())
                .removeValue();
    }

    public void updateCategory(List list, Category category)
    {
        if (category == null || list == null) return;

        refLists.child(list.getId()).child("categories").child(category.getName()).setValue(category);
    }

    public void finishCategory(List list, Category category)
    {
        for (Item item : new ArrayList<>(category.getItems()))
        {
            if (item.isChecked())
            {
                removeItem(list, category, item);
            }
        }

        if (category.isFinished())
        {
            removeCategory(list, category);
        }

    }

    public void addItem(List list, String category_name, Item item, DBCallback callback)
    {
        list.getCategory(category_name).addItem(item);

        refLists.child(list.getId())
                .child("categories")
                .child(category_name)
                .child("items")
                .setValue(list.getCategory(category_name).getItems())
                .addOnSuccessListener(aVoid ->
                {
                    if (callback != null) callback.onComplete();
                })
                .addOnFailureListener(e ->
                {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void removeItem(List list, Category category, Item item)
    {
        removeItem(list, category, item.getBaseName());
    }

    public void removeItem(List list, Category category, String itemName)
    {
        category.removeItem(itemName);
        refLists.child(list.getId())
                .child("categories")
                .child(category.getName())
                .child("items")
                .setValue(category.getItems());

        if (category.isEmpty())
        {
            removeCategory(list, category);
        }
    }

    public void removeItem(List list, String categoryName, Item item)
    {
        Category category = list.getCategory(categoryName);
        removeItem(list, category, item);
    }

    @SuppressWarnings("ExtractMethodRecommender")
    public void sendJoinRequest(String listID, User user, Context context)
    {
        refLists.child(listID).child("requests").get().addOnCompleteListener(task ->
        {
            if (!task.isSuccessful())
            {
                return;
            }

            DataSnapshot snapshot = task.getResult();
            ArrayList<Request> safeRequests;

            try
            {
                safeRequests = snapshot.getValue(
                        new GenericTypeIndicator<>() {
                        }
                );
            }
            catch (DatabaseException e)
            {
                safeRequests = new ArrayList<>();
            }

            if (safeRequests == null)
            {
                safeRequests = new ArrayList<>();
            }

            String userID = user.getId();

            for (Request r : safeRequests)
            {
                if (r.getUserID().equals(userID))
                {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context,
                                    "נשלחה בקשה, מחכה לאישור...",
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
            }

            ArrayList<Request> finalSafeRequests = safeRequests;
            refLists.child(listID).child("userIDs").get().addOnCompleteListener(taskTwo ->
            {
                if (!taskTwo.isSuccessful())
                {
                    return;
                }

                ArrayList<String> userIDs;

                try
                {
                    userIDs = taskTwo.getResult().getValue(
                            new GenericTypeIndicator<>() {
                            }
                    );
                }
                catch (DatabaseException e)
                {
                    userIDs = new ArrayList<>();
                }

                if (userIDs == null) userIDs = new ArrayList<>();

                if (userIDs.contains(userID))
                {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context,
                                    "אתה כבר נמצא ברשימה זו",
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                finalSafeRequests.add(new Request(user));
                refLists.child(listID).child("requests").setValue(finalSafeRequests);

                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context,
                                "נשלחה בקשה, מחכה לאישור...",
                                Toast.LENGTH_SHORT).show()
                );
            });
        });
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
                        ArrayList<String> listIDs;
                        try {
                            listIDs = task.getResult().getValue(
                                    new GenericTypeIndicator<>() {
                                    });
                        } catch (DatabaseException e) {
                            listIDs = new ArrayList<>();
                        }

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
}
