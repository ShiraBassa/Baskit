package com.example.baskit.online_components;

import static com.example.baskit.online_components.FBRefs.refLists;
import static com.example.baskit.online_components.FBRefs.refUsers;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.baskit.Baskit;
import com.example.baskit.R;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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


    public static synchronized void resetInstance()
    {
        instance = null;
    }

    public static synchronized FirebaseDBHandler getInstance()
    {
        if (instance == null)
        {
            instance = new FirebaseDBHandler();
        }

        return instance;
    }

    public ValueEventListener listenToUserName(User user, Consumer<String> callback)
    {
        if (user == null || callback == null || user.getId() == null)
        {
            return null;
        }
        ValueEventListener listener = new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                String new_name = null;

                if (snapshot.exists())
                {
                    new_name = snapshot.getValue(String.class);
                }

                if (!Objects.equals(new_name, user.getName()))
                {
                    callback.accept(new_name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        refUsers.child(user.getId()).child("name").addValueEventListener(listener);

        return listener;
    }

    public ValueEventListener listenToListNames(User user, Consumer<ArrayList<String>> callback)
    {
        if (user == null || callback == null || user.getId() == null)
        {
            callback.accept(new ArrayList<>());
            return null;
        }
        ValueEventListener listener = new ValueEventListener()
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
                if (listIDs != null)
                {
                    listIDs.removeIf(id -> id == null || id.isBlank());
                }

                if (listIDs == null)
                {
                    listNames = new ArrayList<>();
                    callback.accept(listNames);
                    return;
                }

                Set<String> uniqueListIDs = new HashSet<>(listIDs);

                for (String listId : uniqueListIDs)
                {
                    if (listId == null || listId.isBlank())
                    {
                        continue;
                    }
                    refLists.child(listId)
                            .child("name")
                            .get()
                            .addOnCompleteListener(task ->
                            {
                                if (!task.isSuccessful())
                                {
                                    return;
                                }

                                getListNames(user, listNames1 ->
                                        callback.accept(new ArrayList<>(listNames1)));
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        refUsers.child(user.getId()).child("listIDs").addValueEventListener(listener);

        return listener;
    }

    public ValueEventListener listenToList(String listId, GetListCallback callback)
    {
        if (listId == null || listId.isBlank() || callback == null)
        {
            return null;
        }
        ValueEventListener listener = new ValueEventListener()
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
                    if (callback != null)
                    {
                        callback.onListFetched(list);
                    }
                }
                catch (JSONException | IOException e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        refLists.child(listId).addValueEventListener(listener);

        return listener;
    }

    public ValueEventListener listenToCategory(List list, String categoryName, Consumer<Category> callback)
    {
        if (list == null || categoryName == null || callback == null)
        {
            return null;
        }
        ValueEventListener listener = new ValueEventListener()
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

                if (catName == null)
                {
                    catName = Baskit.UNKNOWN_CATEGORY;
                }
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
        };

        refLists.child(list.getId())
                .child("categories")
                .child(categoryName)
                .addValueEventListener(listener);

        return listener;
    }

    public ValueEventListener listenForRequests(List list, Consumer<ArrayList<Request>> callback)
    {
        if (list == null || callback == null || list.getId() == null)
        {
            callback.accept(new ArrayList<>());
            return null;
        }

        ValueEventListener listener = new ValueEventListener()
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

                updatedRequests.removeIf(Objects::isNull);

                list.setRequests(updatedRequests);
                callback.accept(updatedRequests);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        refLists.child(list.getId())
                .child("requests")
                .addValueEventListener(listener);

        return listener;
    }

    public void removeUserNameListener(User user,
                                       ValueEventListener listener)
    {
        if (user == null ||
                user.getId() == null ||
                user.getId().isBlank() ||
                listener == null)
        {
            return;
        }

        refUsers.child(user.getId())
                .child("name")
                .removeEventListener(listener);
    }

    public void removeListNamesListener(User user,
                                        ValueEventListener listener)
    {
        if (user == null ||
                user.getId() == null ||
                user.getId().isBlank() ||
                listener == null)
        {
            return;
        }

        refUsers.child(user.getId())
                .child("listIDs")
                .removeEventListener(listener);
    }

    public void removeListListener(String listId, ValueEventListener listener)
    {
        if (listId == null || listId.isBlank() || listener == null)
        {
            return;
        }

        refLists.child(listId).removeEventListener(listener);
    }

    public void removeCategoryListener(String listId,
                                       String categoryName,
                                       ValueEventListener listener)
    {
        if (listId == null || listId.isBlank() ||
                categoryName == null || categoryName.isBlank() ||
                listener == null)
        {
            return;
        }

        refLists.child(listId)
                .child("categories")
                .child(categoryName)
                .removeEventListener(listener);
    }

    public void removeRequestsListener(String listId,
                                       ValueEventListener listener)
    {
        if (listId == null || listId.isBlank() || listener == null)
        {
            return;
        }

        refLists.child(listId)
                .child("requests")
                .removeEventListener(listener);
    }

    public void changeUserName(User user, String username)
    {
        if (user == null || username == null || username.isBlank())
        {
            return;
        }
        if (Objects.equals(user.getName(), username))
        {
            return;
        }

        refUsers.child(user.getId()).child("name").setValue(username);
    }

    public void getListNames(User user, Consumer<ArrayList<String>> callback)
    {
        if (user == null || callback == null)
        {
            return;
        }
        ArrayList<String> listIDs = user.getListIDs();
        if (listIDs != null)
        {
            listIDs.removeIf(id -> id == null || id.isBlank());
        }

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
            if (listID == null || listID.isBlank())
            {
                completed[0]++;

                if (completed[0] == listIDs.size())
                {
                    callback.accept(finalListNames);
                }

                continue;
            }
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
        if (listId == null || listId.isBlank() || callback == null)
        {
            return;
        }
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
        if (listId == null || listId.isBlank())
        {
            return;
        }
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
        if (list == null || list.getId() == null)
        {
            return;
        }
        ArrayList<String> userIDs = list.getUserIDs();
        if (userIDs == null)
        {
            userIDs = new ArrayList<>();
        }
        for (String userID : userIDs)
        {
            if (userID == null || userID.isBlank())
            {
                continue;
            }
            refUsers.child(userID)
                    .child("listIDs")
                    .runTransaction(new Transaction.Handler()
                    {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData)
                        {
                            ArrayList<String> updatedListIDs = currentData.getValue(
                                    new GenericTypeIndicator<>() {
                                    }
                            );

                            if (updatedListIDs == null)
                            {
                                updatedListIDs = new ArrayList<>();
                            }

                            updatedListIDs.removeIf(id ->
                                    Objects.equals(id, list.getId()));

                            currentData.setValue(updatedListIDs);

                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(DatabaseError error,
                                               boolean committed,
                                               DataSnapshot currentData) {}
                    });
        }

        refLists.child(list.getId()).removeValue();
    }

    public void addList(List list, User user)
    {
        if (list == null || user == null ||
                list.getId() == null || user.getId() == null)
        {
            return;
        }
        refLists.child(list.getId()).setValue(list);

        refUsers.child(user.getId())
                .child("listIDs")
                .runTransaction(new Transaction.Handler()
                {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData)
                    {
                        ArrayList<String> updatedListIDs = currentData.getValue(
                                new GenericTypeIndicator<>() {
                                }
                        );

                        if (updatedListIDs == null)
                        {
                            updatedListIDs = new ArrayList<>();
                        }

                        if (!updatedListIDs.contains(list.getId()))
                        {
                            updatedListIDs.add(list.getId());
                        }

                        currentData.setValue(updatedListIDs);

                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error,
                                           boolean committed,
                                           DataSnapshot currentData) {}
                });
    }

    public void updateList(List list)
    {
        if (list == null || list.getCategories() == null)
        {
            return;
        }
        for (Category category : list.getCategories().values())
        {
            updateCategory(list, category);
        }
    }

    public void finishList(List list)
    {
        if (list == null || list.getCategories() == null)
        {
            return;
        }
        for (Category category : list.getCategories().values())
        {
            finishCategory(list, category);
        }
    }

    public void removeItems(String listID)
    {
        if (listID == null || listID.isBlank())
        {
            return;
        }
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

        refLists.child(listID)
                .child("userIDs")
                .runTransaction(new Transaction.Handler()
                {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData)
                    {
                        ArrayList<String> updatedUserIDs = currentData.getValue(
                                new GenericTypeIndicator<>() {
                                }
                        );

                        if (updatedUserIDs == null)
                        {
                            updatedUserIDs = new ArrayList<>();
                        }

                        updatedUserIDs.removeIf(id ->
                                Objects.equals(id, userID));

                        currentData.setValue(updatedUserIDs);

                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error,
                                           boolean committed,
                                           DataSnapshot currentData) {}
                });

        refUsers.child(userID)
                .child("listIDs")
                .runTransaction(new Transaction.Handler()
                {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData)
                    {
                        ArrayList<String> updatedListIDs = currentData.getValue(
                                new GenericTypeIndicator<>() {
                                }
                        );

                        if (updatedListIDs == null)
                        {
                            updatedListIDs = new ArrayList<>();
                        }

                        updatedListIDs.removeIf(id ->
                                Objects.equals(id, listID));

                        currentData.setValue(updatedListIDs);

                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error,
                                           boolean committed,
                                           DataSnapshot currentData) {}
                });
    }

    public void renameList(String listID, String newName)
    {
        if (listID == null || listID.isBlank() ||
                newName == null || newName.isBlank())
        {
            return;
        }
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
        if (snapshot == null)
        {
            return new List();
        }
        List list = new List();
        list.setId(listId);
        String listName = snapshot.child("name").getValue(String.class);
        list.setName(listName != null ? listName : "");

        // userIDs
        ArrayList<String> userIDs = new ArrayList<>();
        for (DataSnapshot child : snapshot.child("userIDs").getChildren())
        {
            if (child == null)
            {
                continue;
            }
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
            if (catName == null || catName.isBlank())
            {
                catName = Baskit.UNKNOWN_CATEGORY;
            }
            Category cat = new Category(catName);

            Boolean finished = catSnap.child("finished").getValue(Boolean.class);
            if (finished != null) {
                cat.setFinished(finished);
            }

            ArrayList<Item> items = new ArrayList<>();
            DataSnapshot itemsSnap = catSnap.child("items");

            for (DataSnapshot itemSnap : itemsSnap.getChildren())
            {
                if (itemSnap == null)
                {
                    continue;
                }
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
        String key = refLists.push().getKey();
        return key != null ? key : String.valueOf(System.currentTimeMillis());
    }

    public void addCategory(List list, Category category)
    {
        if (list == null || category == null ||
                list.getId() == null || category.getName() == null)
        {
            return;
        }
        list.addCategory(category);

        DatabaseReference categoryRef = refLists
                .child(list.getId())
                .child("categories")
                .child(category.getName());

        categoryRef.setValue(category);
    }

    public void removeCategory(List list, Category category)
    {
        if (list == null || category == null ||
                list.getId() == null || category.getName() == null)
        {
            return;
        }
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
        if (list == null || category == null)
        {
            return;
        }
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
        if (list == null || category_name == null || item == null)
        {
            if (callback != null)
            {
                callback.onFailure(new IllegalArgumentException("Invalid addItem arguments"));
            }
            return;
        }
        Category category = list.getCategory(category_name);

        if (category == null)
        {
            if (callback != null)
            {
                callback.onFailure(new IllegalStateException("Category missing"));
            }
            return;
        }

        refLists.child(list.getId())
                .child("categories")
                .child(category_name)
                .child("items")
                .runTransaction(new Transaction.Handler()
                {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData)
                    {
                        ArrayList<Item> updatedItems;

                        try
                        {
                            updatedItems = currentData.getValue(
                                    new GenericTypeIndicator<>() {
                                    }
                            );
                        }
                        catch (DatabaseException e)
                        {
                            updatedItems = new ArrayList<>();
                        }

                        if (updatedItems == null)
                        {
                            updatedItems = new ArrayList<>();
                        }

                        boolean alreadyExists = false;

                        for (Item existingItem : updatedItems)
                        {
                            if (existingItem == null ||
                                    existingItem.getAbsoluteId() == null ||
                                    item.getAbsoluteId() == null)
                            {
                                continue;
                            }

                            if (Objects.equals(
                                    existingItem.getAbsoluteId(),
                                    item.getAbsoluteId()))
                            {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (!alreadyExists)
                        {
                            updatedItems.add(item);
                        }

                        currentData.setValue(updatedItems);

                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error,
                                           boolean committed,
                                           DataSnapshot currentData)
                    {
                        if (committed && callback != null)
                        {
                            callback.onComplete();
                        }
                        else if (error != null && callback != null)
                        {
                            callback.onFailure(error.toException());
                        }
                    }
                });
    }

    public void removeItem(List list, Category category, Item item)
    {
        removeItem(list, category, item.getBaseName());
    }

    public void removeItem(List list, Category category, String itemName)
    {
        if (list == null || category == null || itemName == null)
        {
            return;
        }

        refLists.child(list.getId())
                .child("categories")
                .child(category.getName())
                .child("items")
                .runTransaction(new Transaction.Handler()
                {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData)
                    {
                        ArrayList<Item> updatedItems;

                        try
                        {
                            updatedItems = currentData.getValue(
                                    new GenericTypeIndicator<>() {
                                    }
                            );
                        }
                        catch (DatabaseException e)
                        {
                            updatedItems = new ArrayList<>();
                        }

                        if (updatedItems == null)
                        {
                            updatedItems = new ArrayList<>();
                        }

                        updatedItems.removeIf(existingItem ->
                                existingItem != null &&
                                        Objects.equals(
                                                existingItem.getBaseName(),
                                                itemName
                                        ));

                        currentData.setValue(updatedItems);

                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error,
                                           boolean committed,
                                           DataSnapshot currentData)
                    {
                        if (committed &&
                                (currentData == null || !currentData.hasChildren()))
                        {
                            removeCategory(list, category);
                        }
                    }
                });
    }

    public void removeItem(List list, String categoryName, Item item)
    {
        Category category = list.getCategory(categoryName);
        if (category == null)
        {
            return;
        }
        removeItem(list, category, item);
    }

    @SuppressWarnings("ExtractMethodRecommender")
    public void sendJoinRequest(String listID, User user, Context context)
    {
        if (listID == null || listID.isBlank() ||
                user == null || context == null)
        {
            return;
        }
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
            if (userID == null || userID.isBlank())
            {
                return;
            }

            for (Request r : safeRequests)
            {
                if (r == null || r.getUserID() == null)
                {
                    continue;
                }
                if (r.getUserID().equals(userID))
                {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context,
                                    Baskit.getAppStr(R.string.list_request_sent),
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
            }

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
                                    Baskit.getAppStr(R.string.list_already_member),
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                refLists.child(listID)
                        .child("requests")
                        .runTransaction(new Transaction.Handler()
                        {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData currentData)
                            {
                                ArrayList<Request> updatedRequests;

                                try
                                {
                                    updatedRequests = currentData.getValue(
                                            new GenericTypeIndicator<>() {
                                            }
                                    );
                                }
                                catch (DatabaseException e)
                                {
                                    updatedRequests = new ArrayList<>();
                                }

                                if (updatedRequests == null)
                                {
                                    updatedRequests = new ArrayList<>();
                                }

                                boolean alreadyExists = false;

                                for (Request existingRequest : updatedRequests)
                                {
                                    if (existingRequest == null ||
                                            existingRequest.getUserID() == null)
                                    {
                                        continue;
                                    }

                                    if (Objects.equals(existingRequest.getUserID(), userID))
                                    {
                                        alreadyExists = true;
                                        break;
                                    }
                                }

                                if (!alreadyExists)
                                {
                                    updatedRequests.add(new Request(user));
                                }

                                currentData.setValue(updatedRequests);

                                return Transaction.success(currentData);
                            }

                            @Override
                            public void onComplete(DatabaseError error,
                                                   boolean committed,
                                                   DataSnapshot currentData)
                            {
                                if (committed)
                                {
                                    new Handler(Looper.getMainLooper()).post(() ->
                                            Toast.makeText(context,
                                                    Baskit.getAppStr(R.string.list_request_sent),
                                                    Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                        });
            });
        });
    }

    public void acceptRequest(List list, Request request)
    {
        if (list == null || request == null || request.getUserID() == null)
        {
            return;
        }
        removeRequest(list, request);

        refLists.child(list.getId())
                .child("userIDs")
                .runTransaction(new Transaction.Handler()
                {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData)
                    {
                        ArrayList<String> userIDs = currentData.getValue(
                                new GenericTypeIndicator<>() {
                                }
                        );

                        if (userIDs == null)
                        {
                            userIDs = new ArrayList<>();
                        }

                        if (!userIDs.contains(request.getUserID()))
                        {
                            userIDs.add(request.getUserID());
                        }

                        currentData.setValue(userIDs);

                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error,
                                           boolean committed,
                                           DataSnapshot currentData) {}
                });

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

                        refUsers.child(request.getUserID())
                                .child("listIDs")
                                .runTransaction(new Transaction.Handler()
                                {
                                    @NonNull
                                    @Override
                                    public Transaction.Result doTransaction(@NonNull MutableData currentData)
                                    {
                                        ArrayList<String> updatedListIDs = currentData.getValue(
                                                new GenericTypeIndicator<>() {
                                                }
                                        );

                                        if (updatedListIDs == null)
                                        {
                                            updatedListIDs = new ArrayList<>();
                                        }

                                        if (!updatedListIDs.contains(list.getId()))
                                        {
                                            updatedListIDs.add(list.getId());
                                        }

                                        currentData.setValue(updatedListIDs);

                                        return Transaction.success(currentData);
                                    }

                                    @Override
                                    public void onComplete(DatabaseError error,
                                                           boolean committed,
                                                           DataSnapshot currentData) {}
                                });
                    }
                });
    }

    public void declineRequest(List list, Request request)
    {
        if (list == null || request == null)
        {
            return;
        }
        removeRequest(list, request);
    }

    private void removeRequest(List list, Request request)
    {
        if (list == null || request == null ||
                list.getId() == null || request.getUserID() == null)
        {
            return;
        }

        refLists.child(list.getId())
                .child("requests")
                .runTransaction(new Transaction.Handler()
                {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData)
                    {
                        ArrayList<Request> updatedRequests;

                        try
                        {
                            updatedRequests = currentData.getValue(
                                    new GenericTypeIndicator<>() {
                                    }
                            );
                        }
                        catch (DatabaseException e)
                        {
                            updatedRequests = new ArrayList<>();
                        }

                        if (updatedRequests == null)
                        {
                            updatedRequests = new ArrayList<>();
                        }

                        String requestUserId = request.getUserID();

                        updatedRequests.removeIf(existingRequest ->
                                existingRequest != null &&
                                        Objects.equals(existingRequest.getUserID(), requestUserId));

                        currentData.setValue(updatedRequests);

                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error,
                                           boolean committed,
                                           DataSnapshot currentData) {}
                });
    }
}