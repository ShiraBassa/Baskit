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

        // Current session
        currUser.reload().addOnCompleteListener(task ->
        {
            // No current session
            if (!task.isSuccessful())
            {
                refAuth.signOut();
                postError(callback, "Session expired. Please log in again.", ErrorType.GENERAL);
                return;
            }

            // There is a current session -> user info
            refUsers.child(currUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot)
                        {
                            if (snapshot.exists()) //There is info about the current user in the firebase
                            {
                                user = snapshot.getValue(User.class); // Unwrap the info

                                // Get the session token
                                currUser.getIdToken(false).addOnCompleteListener(taskTwo ->
                                {
                                    if (taskTwo.isSuccessful()) // There is a token
                                    {
                                        String idToken = taskTwo.getResult().getToken();
                                        user.setToken(idToken);

                                        ArrayList<String> listIDs = user.getListIDs();

                                        // There are lists
                                        if (listIDs != null)
                                        {
                                            user.setListIDs(listIDs);

                                            // Correcting the interrupted listIDs snapshots
                                            if (!listIDs.isEmpty())
                                            {
                                                listIDs.removeIf(Objects::isNull); // Remove the null object lists (no actual list, not referring to an empty list but to an broken one)
                                                user.setListIDs(listIDs);

                                                // Update the firebase
                                                if (listIDs.isEmpty())
                                                {
                                                    refUsers.child(user.getId()).child("listIDs").removeValue();
                                                }
                                                else
                                                {
                                                    refUsers.child(user.getId()).child("listIDs").setValue(listIDs);
                                                }
                                            }
                                        }

                                        // Login to the server
                                        new Thread(() ->
                                        {
                                            boolean ok = apiHandler.login(idToken);
                                            new Handler(Looper.getMainLooper()).post(() ->
                                            {
                                                if (ok) callback.onAuthSuccess();
                                                else callback.onAuthError("Server login failed", ErrorType.GENERAL);
                                            });
                                        }).start();
                                    }
                                    else // There is no token
                                    {
                                        postError(callback, "Failed to get session token. Please try again.", ErrorType.GENERAL);
                                    }
                                });
                            }
                            else //There is no info about the current user in the firebase
                            {
                                user = new User(currUser.getUid(), currUser.getEmail());

                                currUser.getIdToken(false).addOnCompleteListener(taskTwo ->
                                {
                                    if (taskTwo.isSuccessful())
                                    {
                                        String idToken = taskTwo.getResult().getToken();
                                        user.setToken(idToken);

                                        new Thread(() ->
                                        {
                                            boolean loginSuccess = apiHandler.login(user.getToken());

                                            if (!loginSuccess)
                                            {
                                                postError(callback, "Server login failed", ErrorType.GENERAL);
                                                return;
                                            }

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

                        firebaseUser.getIdToken(false).addOnCompleteListener(taskTwo ->
                        {
                            if (taskTwo.isSuccessful())
                            {
                                String idToken = taskTwo.getResult().getToken();
                                user.setToken(idToken);

                                new Thread(() ->
                                {
                                    boolean loginSuccess = apiHandler.login(user.getToken());

                                    if (!loginSuccess)
                                    {
                                        postError(callback, "Server login failed", ErrorType.GENERAL);
                                        return;
                                    }

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

                                    firebaseUser.getIdToken(false).addOnCompleteListener(taskTwo ->
                                    {
                                        if (taskTwo.isSuccessful())
                                        {
                                            String idToken = taskTwo.getResult().getToken();
                                            user.setToken(idToken);

                                            new Thread(() ->
                                            {
                                                boolean ok = apiHandler.login(user.getToken());

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

                                    firebaseUser.getIdToken(false).addOnCompleteListener(taskTwo ->
                                    {
                                        if(taskTwo.isSuccessful())
                                        {
                                            String idToken = taskTwo.getResult().getToken();
                                            user.setToken(idToken);

                                            new Thread(() ->
                                            {
                                                boolean loginSuccess = apiHandler.login(user.getToken());

                                                if (!loginSuccess)
                                                {
                                                    postError(callback, "Server login failed", ErrorType.GENERAL);
                                                    return;
                                                }

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

    public void setSupermarkets(Map<String, ArrayList<String>> supermarkets, Runnable onComplete)
    {
        try
        {
            apiHandler.setBranches(supermarkets);
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
    }

    public void addSupermarketSection(Supermarket supermarket, Runnable onComplete)
    {
        try
        {
            Map<String, ArrayList<String>> branches = apiHandler.getChoices();
            String supermarketName = supermarket.getSupermarket();
            String sectionName = supermarket.getSection();

            if (!branches.containsKey(supermarketName) || branches.get(supermarketName) == null) {
                branches.put(supermarketName, new ArrayList<>());
            }

            branches.get(supermarketName).add(sectionName);
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
    }

    public void removeSupermarketSection(Supermarket supermarket, Runnable onComplete)
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