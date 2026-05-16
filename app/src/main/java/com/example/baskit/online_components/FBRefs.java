package com.example.baskit.online_components;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FBRefs
{
    // Auth
    protected static final FirebaseAuth refAuth = FirebaseAuth.getInstance();

    // Realtime DataBase
    public static final FirebaseDatabase FBDB = FirebaseDatabase.getInstance();
    public static final DatabaseReference refUsers = FBDB.getReference("Users");
    public static final DatabaseReference refLists = FBDB.getReference("Lists");
}
