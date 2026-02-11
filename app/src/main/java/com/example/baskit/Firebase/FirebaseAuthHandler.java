package com.example.baskit.Firebase;

import static com.example.baskit.Firebase.FBRefs.refAuth;
import static com.example.baskit.Firebase.FBRefs.refUsers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.baskit.API.APIHandler;
import com.example.baskit.Login.ErrorType;
import com.example.baskit.MainComponents.Supermarket;
import com.example.baskit.MainComponents.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FirebaseAuthHandler
{
    private static FirebaseAuthHandler instance;
    private static User user;
    private final APIHandler apiHandler = APIHandler.getInstance();

    public interface AuthCallback
    {
        void onAuthSuccess();
        void onAuthError(String msg, ErrorType type);
    }

    public static FirebaseAuthHandler getInstance()
    {
        if (instance == null)
        {
            instance = new FirebaseAuthHandler();
        }

        return instance;
    }

    public static void resetInstance()
    {
        user = null;
        instance = null;
        instance = new FirebaseAuthHandler();
    }

    private void postSuccess(AuthCallback callback)
    {
        new Handler(Looper.getMainLooper()).post(callback::onAuthSuccess);
    }

    private void postError(AuthCallback callback, String msg, ErrorType type)
    {
        new Handler(Looper.getMainLooper()).post(() -> callback.onAuthError(msg, type));
    }

    public void checkCurrUser(AuthCallback callback)
    {
        FirebaseUser currUser = refAuth.getCurrentUser();

        if (currUser == null)
        {
            postError(callback, "", ErrorType.NOT_LOGGED);
            return;
        }

        currUser.reload().addOnCompleteListener(task ->
        {
            if (!task.isSuccessful())
            {
                refAuth.signOut();
                postError(callback, "Session expired. Please log in again.", ErrorType.GENERAL);
                return;
            }

            refUsers.child(currUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot)
                        {
                            if (snapshot.exists())
                            {
                                user = snapshot.getValue(User.class);

                                currUser.getIdToken(true).addOnCompleteListener(taskTwo ->
                                {
                                    if (taskTwo.isSuccessful())
                                    {
                                        String idToken = taskTwo.getResult().getToken();
                                        user.setToken(idToken);

                                        ArrayList<String> listIDs = user.getListIDs();

                                        if (listIDs != null)
                                        {
                                            listIDs.removeIf(Objects::isNull);
                                            user.setListIDs(listIDs);

                                            if (listIDs.isEmpty())
                                            {
                                                refUsers.child(user.getId()).child("listIDs").removeValue();
                                            }
                                            else
                                            {
                                                refUsers.child(user.getId()).child("listIDs").setValue(listIDs);
                                            }
                                        }

                                        new Thread(() ->
                                        {
                                            boolean ok = apiHandler.setFirebaseToken(idToken);
                                            new Handler(Looper.getMainLooper()).post(() ->
                                            {
                                                if (ok) callback.onAuthSuccess();
                                                else callback.onAuthError("Server login failed", ErrorType.GENERAL);
                                            });
                                        }).start();
                                    }
                                    else
                                    {
                                        postError(callback, "Failed to get session token. Please try again.", ErrorType.GENERAL);
                                    }
                                });
                            }
                            else
                            {
                                user = new User(currUser.getUid(), currUser.getEmail());

                                currUser.getIdToken(true).addOnCompleteListener(taskTwo ->
                                {
                                    if (taskTwo.isSuccessful())
                                    {
                                        String idToken = taskTwo.getResult().getToken();
                                        user.setToken(idToken);

                                        new Thread(() ->
                                        {
                                            boolean loginSuccess = apiHandler.setFirebaseToken(user.getToken());

                                            if (!loginSuccess)
                                            {
                                                postError(callback, "Server login failed", ErrorType.GENERAL);
                                                return;
                                            }

                                            getUserInfo();

                                            refUsers.child(user.getId()).setValue(user)
                                                    .addOnCompleteListener(taskDB ->
                                                    {
                                                        if (taskDB.isSuccessful())
                                                        {
                                                            postSuccess(callback);
                                                        }
                                                        else
                                                        {
                                                            postError(callback, "Database error", ErrorType.GENERAL);
                                                        }
                                                    });
                                        }).start();
                                    }
                                    else
                                    {
                                        postError(callback, "Failed to get session token. Please try again.", ErrorType.GENERAL);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error)
                        {
                            postError(callback, "DB error: " + error.getMessage(), ErrorType.GENERAL);
                        }
                    });
        });
    }

    public void signInOrSignUp(String email, String password, AuthCallback callback)
    {
        refAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task ->
                {
                    if (task.isSuccessful())
                    {
                        FirebaseUser firebaseUser = refAuth.getCurrentUser();

                        if (firebaseUser == null)
                        {
                            postError(callback, "Signup failed: no user info", ErrorType.GENERAL);
                            return;
                        }

                        user = new User(firebaseUser.getUid(), email);

                        firebaseUser.getIdToken(true).addOnCompleteListener(taskTwo ->
                        {
                            if (taskTwo.isSuccessful())
                            {
                                String idToken = taskTwo.getResult().getToken();
                                user.setToken(idToken);

                                new Thread(() ->
                                {
                                    boolean loginSuccess = apiHandler.setFirebaseToken(user.getToken());

                                    if (!loginSuccess)
                                    {
                                        postError(callback, "Server login failed", ErrorType.GENERAL);
                                        return;
                                    }

                                    getUserInfo();

                                    refUsers.child(user.getId()).setValue(user)
                                            .addOnCompleteListener(taskDB ->
                                            {
                                                if (taskDB.isSuccessful())
                                                {
                                                    postSuccess(callback);
                                                }
                                                else
                                                {
                                                    postError(callback, "Database error", ErrorType.GENERAL);
                                                }
                                            });
                                }).start();
                            }
                            else
                            {
                                postError(callback, "Failed to get session token. Please try again.", ErrorType.GENERAL);
                            }
                        });
                    }
                    else
                    {
                        Exception e = task.getException();

                        if (e instanceof FirebaseNetworkException)
                        {
                            postError(callback, "Network error. Please check your connection", ErrorType.GENERAL);
                        }
                        else if (e instanceof FirebaseAuthUserCollisionException)
                        {
                            // Already exists → try login instead
                            signIn(email, password, callback);
                        }
                        else if (e instanceof FirebaseAuthInvalidUserException)
                        {
                            postError(callback, "Invalid email format", ErrorType.EMAIL);
                        }
                        else if (e instanceof FirebaseAuthWeakPasswordException)
                        {
                            postError(callback, "Password too weak", ErrorType.PASSWORD);
                        }
                        else if (e instanceof FirebaseAuthInvalidCredentialsException)
                        {
                            postError(callback, "General authentication failure", ErrorType.GENERAL);
                        }
                        else if (e instanceof FirebaseException)
                        {
                            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

                            if (msg.contains("password"))
                            {
                                postError(callback, "Password too weak", ErrorType.PASSWORD);
                            }
                            else if (msg.contains("email"))
                            {
                                postError(callback, "Invalid email format", ErrorType.EMAIL);
                            }
                            else if (msg.contains("network"))
                            {
                                postError(callback, "Network error. Please check your connection", ErrorType.GENERAL);
                            }
                            else
                            {
                                postError(callback, "An error occurred. Please try again later", ErrorType.GENERAL);
                            }
                        }
                        else
                        {
                            postError(callback, "An error occurred. Please try again later", ErrorType.GENERAL);
                        }
                    }
                });
    }

    private void signIn(String email, String password, AuthCallback callback)
    {
        refAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task ->
                {
                    if (!task.isSuccessful())
                    {
                        Exception e = task.getException();

                        if (e instanceof FirebaseAuthInvalidCredentialsException)
                        {
                            postError(callback, "Invalid password", ErrorType.PASSWORD);
                        }
                        else if (e instanceof FirebaseNetworkException)
                        {
                            postError(callback, "Network error. Please check your connection", ErrorType.GENERAL);
                        }
                        else
                        {
                            postError(callback, "An error occurred. Please try again later", ErrorType.GENERAL);
                        }

                        return;
                    }

                    FirebaseUser firebaseUser = refAuth.getCurrentUser();

                    if (firebaseUser == null)
                    {
                        postError(callback, "Login failed: no user info", ErrorType.GENERAL);
                        return;
                    }

                    refUsers.child(firebaseUser.getUid()).get()
                            .addOnCompleteListener(taskDB ->
                            {
                                if (taskDB.isSuccessful() && taskDB.getResult().exists())
                                {
                                    user = taskDB.getResult().getValue(User.class);

                                    firebaseUser.getIdToken(true).addOnCompleteListener(taskTwo ->
                                    {
                                        if (taskTwo.isSuccessful())
                                        {
                                            String idToken = taskTwo.getResult().getToken();
                                            user.setToken(idToken);

                                            new Thread(() ->
                                            {
                                                boolean ok = apiHandler.setFirebaseToken(user.getToken());

                                                if (!ok)
                                                {
                                                    postError(callback, "Server login failed", ErrorType.GENERAL);
                                                    return;
                                                }

                                                postSuccess(callback);
                                            }).start();
                                        }
                                        else
                                        {
                                            postError(callback, "Failed to get session token. Please try again.", ErrorType.GENERAL);
                                        }
                                    });
                                }
                                else
                                {
                                    user = new User(firebaseUser.getUid(), firebaseUser.getEmail());

                                    firebaseUser.getIdToken(true).addOnCompleteListener(taskTwo ->
                                    {
                                        if(taskTwo.isSuccessful())
                                        {
                                            String idToken = taskTwo.getResult().getToken();
                                            user.setToken(idToken);

                                            new Thread(() ->
                                            {
                                                boolean loginSuccess = apiHandler.setFirebaseToken(user.getToken());

                                                if (!loginSuccess)
                                                {
                                                    postError(callback, "Server login failed", ErrorType.GENERAL);
                                                    return;
                                                }

                                                getUserInfo();

                                                refUsers.child(user.getId()).setValue(user)
                                                        .addOnCompleteListener(taskDBTwo ->
                                                        {
                                                            if (taskDBTwo.isSuccessful())
                                                            {
                                                                postSuccess(callback);
                                                            }
                                                            else
                                                            {
                                                                postError(callback, "Database error", ErrorType.GENERAL);
                                                            }
                                                        });
                                            }).start();
                                        }
                                        else
                                        {
                                            postError(callback, "Failed to get session token. Please try again.", ErrorType.GENERAL);
                                        }
                                    });
                                }
                            });
                });
    }

    public User getUser()
    {
        return user;
    }

    public void logOut()
    {
        refAuth.signOut();
        resetInstance();
        apiHandler.resetInstance();
    }

    private void getUserInfo()
    {
        if (user == null)
        {
            Log.e("GET_USER_INFO", "User is null");
            return;
        }

        user.setName("משתמש");

        try
        {
            ArrayList<String> all_cities = apiHandler.getAllCities();
            if (all_cities == null || all_cities.isEmpty())
            {
                Log.e("GET_USER_INFO", "No cities returned from server");
                return;
            }

            ArrayList<String> cities = new ArrayList<>();
            cities.add(all_cities.get(0));
            apiHandler.setCities(cities);

            ArrayList<String> all_stores = apiHandler.getStores();
            if (all_stores == null || all_stores.isEmpty())
            {
                Log.e("GET_USER_INFO", "No stores returned from server");
                return;
            }

            ArrayList<String> stores = new ArrayList<>();
            stores.add(all_stores.get(0));
            apiHandler.setStores(stores);

            Map<String, ArrayList<String>> all_branches = apiHandler.getBranches();
            if (all_branches == null)
            {
                Log.e("GET_USER_INFO", "No branches map returned from server");
                return;
            }

            Map<String, ArrayList<String>> branches = new HashMap<>();
            String firstStore = stores.get(0);
            ArrayList<String> firstStoreBranches = all_branches.get(firstStore);

            if (firstStoreBranches == null || firstStoreBranches.isEmpty())
            {
                Log.e("GET_USER_INFO", "No branches returned for store=" + firstStore);
                return;
            }

            ArrayList<String> selectedBranches = new ArrayList<>();
            selectedBranches.add(firstStoreBranches.get(0));
            if (firstStoreBranches.size() > 1)
            {
                selectedBranches.add(firstStoreBranches.get(1));
            }

            branches.put(firstStore, selectedBranches);
            apiHandler.setBranches(branches);
        }
        catch (IOException | JSONException e)
        {
            e.printStackTrace();
            Log.e("GET_USER_INFO", "Error loading user info", e);
        }
    }

    public void addSupermarketSection(Supermarket supermarket, Runnable onComplete)
    {
        new Thread(() ->
        {
            try
            {
                Map<String, ArrayList<String>> branches = apiHandler.getChoices();
                String marketName = supermarket.getSupermarket();
                String sectionName = supermarket.getSection();

                // Initialize the list if it doesn't exist
                if (!branches.containsKey(marketName) || branches.get(marketName) == null) {
                    branches.put(marketName, new ArrayList<>());
                }

                branches.get(marketName).add(sectionName);
                apiHandler.setBranches(branches);
                apiHandler.updateSupermarkets();
                apiHandler.reset();

                if (onComplete != null)
                {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            }
            catch (IOException | JSONException e)
            {
                e.printStackTrace();
            }
        }).start();
    }

    public void removeSupermarketSection(Supermarket supermarket, Runnable onComplete)
    {
        new Thread(() ->
        {
            try
            {
                Map<String, ArrayList<String>> branches = apiHandler.getChoices();
                String supermarketName = supermarket.getSupermarket();
                Objects.requireNonNull(branches.get(supermarketName)).remove(supermarket.getSection());

                if (Objects.requireNonNull(branches.get(supermarketName)).isEmpty())
                {
                    branches.remove(supermarketName);
                }

                apiHandler.setBranches(branches);
                apiHandler.updateSupermarkets();
                apiHandler.reset();

                if (onComplete != null)
                {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            }
            catch (IOException | JSONException e)
            {
                Log.e("Remove supermarket", e.getMessage());
            }
        }).start();
    }

    public void addCity(String city, Runnable onComplete)
    {
        new Thread(() ->
        {
            try
            {
                ArrayList<String> cities = apiHandler.getCities();

                if (cities.contains(city))
                {
                    return;
                }

                cities.add(city);
                apiHandler.setCities(cities);
                apiHandler.reset();

                if (onComplete != null)
                {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            }
            catch (IOException | JSONException e)
            {
                e.printStackTrace();
            }
        }).start();
    }

    public void removeCity(String city, Runnable onComplete)
    {
        new Thread(() ->
        {
            try
            {
                ArrayList<String> cities = apiHandler.getCities();

                if (!cities.contains(city))
                {
                    return;
                }

                cities.remove(city);
                apiHandler.setCities(cities);
                apiHandler.reset();

                if (onComplete != null)
                {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            }
            catch (IOException | JSONException e)
            {
                e.printStackTrace();
            }
        }).start();
    }
}