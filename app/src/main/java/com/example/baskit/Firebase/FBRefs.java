package com.example.baskit.Firebase;

import com.example.baskit.MainComponents.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FBRefs
{
    // Auth
    protected static FirebaseAuth refAuth = FirebaseAuth.getInstance();

    // Realtime DataBase
    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();
    public static DatabaseReference refUsers = FBDB.getReference("Users");
    public static DatabaseReference refLists = FBDB.getReference("Lists");
}
