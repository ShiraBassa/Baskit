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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirebaseAuthHandler
{
    private static FirebaseAuthHandler instance;
    private static User user;
    private final APIHandler apiHandler = APIHandler.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();

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
        if (instance != null)
        {
            try { instance.bg.shutdownNow(); } catch (Exception ignored) {}
        }

        user = null;
        instance = null;
        instance = new FirebaseAuthHandler();
    }

    private void finishAuthSuccess(AuthCallback callback)
    {
        // Always load local cache first so UI can render even if the device disconnects right after.
        bg.execute(() ->
        {
            try
            {
                apiHandler.loadFromDbOnly();
            }
            catch (Exception e)
            {
                Log.e("FirebaseAuthHandler", "loadFromDbOnly failed", e);
            }

            mainHandler.post(callback::onAuthSuccess);
        });
    }

    private void finishAuthError(AuthCallback callback, String msg, ErrorType type)
    {
        mainHandler.post(() -> callback.onAuthError(msg, type));
    }

    public void checkCurrUser(AuthCallback callback)
    {
        FirebaseUser currUser = refAuth.getCurrentUser();

        if (currUser == null)
        {
            finishAuthError(callback, "", ErrorType.NOT_LOGGED);
            return;
        }

        currUser.reload().addOnCompleteListener(task ->
        {
            if (!task.isSuccessful())
            {
                refAuth.signOut();
                finishAuthError(callback, "Session expired. Please log in again.", ErrorType.GENERAL);
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

                                        bg.execute(() ->
                                        {
                                            try { apiHandler.setFirebaseToken(idToken); } catch (Exception ignored) {}
                                            // Wait for local SQL cache load before success
                                            try { apiHandler.loadFromDbOnly(); } catch (Exception e) { Log.e("FirebaseAuthHandler", "loadFromDbOnly failed", e); }
                                            mainHandler.post(callback::onAuthSuccess);
                                        });
                                    }
                                    else
                                    {
                                        finishAuthSuccess(callback);
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

                                        bg.execute(() ->
                                        {
                                            try { apiHandler.setFirebaseToken(idToken); } catch (Exception ignored) {}

                                            // Try to initialize defaults (best-effort; may fail offline)
                                            try { getUserInfo(); } catch (Exception e) { Log.e("FirebaseAuthHandler", "getUserInfo failed", e); }

                                            try { refUsers.child(user.getId()).setValue(user); } catch (Exception ignored) {}

                                            // Ensure local SQL cache is loaded before continuing
                                            try { apiHandler.loadFromDbOnly(); } catch (Exception e) { Log.e("FirebaseAuthHandler", "loadFromDbOnly failed", e); }

                                            mainHandler.post(callback::onAuthSuccess);
                                        });
                                    }
                                    else
                                    {
                                        finishAuthSuccess(callback);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error)
                        {
                            finishAuthError(callback, "DB error: " + error.getMessage(), ErrorType.GENERAL);
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
                            finishAuthError(callback, "Signup failed: no user info", ErrorType.GENERAL);
                            return;
                        }

                        user = new User(firebaseUser.getUid(), email);

                        firebaseUser.getIdToken(true).addOnCompleteListener(taskTwo ->
                        {
                            if (taskTwo.isSuccessful())
                            {
                                String idToken = taskTwo.getResult().getToken();
                                user.setToken(idToken);

                                bg.execute(() ->
                                {
                                    try { apiHandler.setFirebaseToken(idToken); } catch (Exception ignored) {}
                                    try { getUserInfo(); } catch (Exception e) { Log.e("FirebaseAuthHandler", "getUserInfo failed", e); }

                                    try { refUsers.child(user.getId()).setValue(user); } catch (Exception ignored) {}

                                    // Load local SQL cache before success
                                    try { apiHandler.loadFromDbOnly(); } catch (Exception e) { Log.e("FirebaseAuthHandler", "loadFromDbOnly failed", e); }

                                    mainHandler.post(callback::onAuthSuccess);
                                });
                            }
                            else
                            {
                                finishAuthSuccess(callback);
                            }
                        });
                    }
                    else
                    {
                        Exception e = task.getException();

                        if (e instanceof FirebaseNetworkException)
                        {
                            finishAuthError(callback, "Network error. Please check your connection", ErrorType.GENERAL);
                        }
                        else if (e instanceof FirebaseAuthUserCollisionException)
                        {
                            // Already exists → try login instead
                            signIn(email, password, callback);
                        }
                        else if (e instanceof FirebaseAuthInvalidUserException)
                        {
                            finishAuthError(callback, "Invalid email format", ErrorType.EMAIL);
                        }
                        else if (e instanceof FirebaseAuthWeakPasswordException)
                        {
                            finishAuthError(callback, "Password too weak", ErrorType.PASSWORD);
                        }
                        else if (e instanceof FirebaseAuthInvalidCredentialsException)
                        {
                            finishAuthError(callback, "General authentication failure", ErrorType.GENERAL);
                        }
                        else if (e instanceof FirebaseException)
                        {
                            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

                            if (msg.contains("password"))
                            {
                                finishAuthError(callback, "Password too weak", ErrorType.PASSWORD);
                            }
                            else if (msg.contains("email"))
                            {
                                finishAuthError(callback, "Invalid email format", ErrorType.EMAIL);
                            }
                            else if (msg.contains("network"))
                            {
                                finishAuthError(callback, "Network error. Please check your connection", ErrorType.GENERAL);
                            }
                            else
                            {
                                finishAuthError(callback, "An error occurred. Please try again later", ErrorType.GENERAL);
                            }
                        }
                        else
                        {
                            finishAuthError(callback, "An error occurred. Please try again later", ErrorType.GENERAL);
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
                            finishAuthError(callback, "Invalid password", ErrorType.PASSWORD);
                        }
                        else if (e instanceof FirebaseNetworkException)
                        {
                            finishAuthError(callback, "Network error. Please check your connection", ErrorType.GENERAL);
                        }
                        else
                        {
                            finishAuthError(callback, "An error occurred. Please try again later", ErrorType.GENERAL);
                        }

                        return;
                    }

                    FirebaseUser firebaseUser = refAuth.getCurrentUser();

                    if (firebaseUser == null)
                    {
                        finishAuthError(callback, "Login failed: no user info", ErrorType.GENERAL);
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

                                            bg.execute(() ->
                                            {
                                                try { apiHandler.setFirebaseToken(idToken); } catch (Exception ignored) {}
                                                // Load local SQL cache before success
                                                try { apiHandler.loadFromDbOnly(); } catch (Exception e) { Log.e("FirebaseAuthHandler", "loadFromDbOnly failed", e); }
                                                mainHandler.post(callback::onAuthSuccess);
                                            });
                                        }
                                        else
                                        {
                                            finishAuthSuccess(callback);
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

                                            bg.execute(() ->
                                            {
                                                try { apiHandler.setFirebaseToken(idToken); } catch (Exception ignored) {}
                                                try { getUserInfo(); } catch (Exception e) { Log.e("FirebaseAuthHandler", "getUserInfo failed", e); }

                                                try { refUsers.child(user.getId()).setValue(user); } catch (Exception ignored) {}

                                                // Load local SQL cache before success
                                                try { apiHandler.loadFromDbOnly(); } catch (Exception e) { Log.e("FirebaseAuthHandler", "loadFromDbOnly failed", e); }
                                                mainHandler.post(callback::onAuthSuccess);
                                            });
                                        }
                                        else
                                        {
                                            finishAuthSuccess(callback);
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
        user.setName("משתמש");

        try
        {
            ArrayList<String> all_cities = apiHandler.getAllCities();
            if (all_cities == null || all_cities.isEmpty()) return;

            ArrayList<String> cities = new ArrayList<>();
            cities.add(all_cities.get(0));
            apiHandler.setCities(cities);

            ArrayList<String> all_stores = apiHandler.getStores();
            if (all_stores == null || all_stores.isEmpty()) return;

            ArrayList<String> stores = new ArrayList<>();
            stores.add(all_stores.get(0));
            apiHandler.setStores(stores);

            Map<String, ArrayList<String>> all_branches = apiHandler.getBranches();
            if (all_branches == null) return;

            ArrayList<String> branchesForStore = all_branches.get(stores.get(0));
            if (branchesForStore == null || branchesForStore.isEmpty()) return;

            Map<String, ArrayList<String>> branches = new HashMap<>();
            ArrayList<String> picked = new ArrayList<>();
            picked.add(branchesForStore.get(0));
            if (branchesForStore.size() > 1) picked.add(branchesForStore.get(1));

            branches.put(stores.get(0), picked);
            apiHandler.setBranches(branches);
        }
        catch (IOException | JSONException e)
        {
            Log.e("FirebaseAuthHandler", "getUserInfo failed", e);
        }
    }

    public void addSupermarketSection(Supermarket supermarket, Runnable onComplete)
    {
        bg.execute(() ->
        {
            try
            {
                Map<String, ArrayList<String>> branches;
                try
                {
                    branches = apiHandler.getChoices();
                }
                catch (Exception e)
                {
                    Log.e("FirebaseAuthHandler", "getChoices failed (addSupermarketSection)", e);
                    branches = apiHandler.getBranches();
                }

                if (branches == null) branches = new HashMap<>();

                if (supermarket == null) return;
                String marketName = supermarket.getSupermarket();
                String sectionName = supermarket.getSection();
                if (marketName == null || sectionName == null) return;

                // Initialize the list if it doesn't exist
                if (!branches.containsKey(marketName) || branches.get(marketName) == null) {
                    branches.put(marketName, new ArrayList<>());
                }

                if (!branches.get(marketName).contains(sectionName))
                {
                    branches.get(marketName).add(sectionName);
                }
                apiHandler.setBranches(branches);
                apiHandler.updateSupermarkets();

                if (onComplete != null)
                {
                    mainHandler.post(onComplete);
                }
            }
            catch (IOException | JSONException e)
            {
                Log.e("FirebaseAuthHandler", "addSupermarketSection failed", e);
            }
        });
    }

    public void removeSupermarketSection(Supermarket supermarket, Runnable onComplete)
    {
        bg.execute(() ->
        {
            try
            {
                Map<String, ArrayList<String>> branches;
                try
                {
                    branches = apiHandler.getChoices();
                }
                catch (Exception e)
                {
                    Log.e("FirebaseAuthHandler", "getChoices failed (removeSupermarketSection)", e);
                    branches = apiHandler.getBranches();
                }

                if (branches == null) return;

                if (supermarket == null) return;
                String supermarketName = supermarket.getSupermarket();
                String section = supermarket.getSection();
                if (supermarketName == null || section == null) return;

                ArrayList<String> list = branches.get(supermarketName);
                if (list == null) return;

                list.remove(section);
                if (list.isEmpty())
                {
                    branches.remove(supermarketName);
                }

                apiHandler.setBranches(branches);
                apiHandler.updateSupermarkets();

                if (onComplete != null)
                {
                    mainHandler.post(onComplete);
                }
            }
            catch (IOException | JSONException e)
            {
                Log.e("FirebaseAuthHandler", "removeSupermarketSection failed", e);
            }
        });
    }

    public void addCity(String city, Runnable onComplete)
    {
        bg.execute(() ->
        {
            try
            {
                if (city == null) return;
                ArrayList<String> cities = apiHandler.getCities();
                if (cities == null) cities = new ArrayList<>();

                if (!cities.contains(city))
                {
                    cities.add(city);
                    apiHandler.setCities(cities);
                }

                if (onComplete != null)
                {
                    mainHandler.post(onComplete);
                }
            }
            catch (IOException | JSONException e)
            {
                Log.e("FirebaseAuthHandler", "addCity failed", e);
            }
        });
    }

    public void removeCity(String city, Runnable onComplete)
    {
        bg.execute(() ->
        {
            try
            {
                if (city == null) return;
                ArrayList<String> cities = apiHandler.getCities();
                if (cities == null) return;

                if (cities.remove(city))
                {
                    apiHandler.setCities(cities);
                }

                if (onComplete != null)
                {
                    mainHandler.post(onComplete);
                }
            }
            catch (IOException | JSONException e)
            {
                Log.e("FirebaseAuthHandler", "removeCity failed", e);
            }
        });
    }
}