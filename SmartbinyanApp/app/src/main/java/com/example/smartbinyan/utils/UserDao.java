package com.example.smartbinyan.utils;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {

    // Insert a new user
    @Insert
    void insert(User user);

    // Find a user by username
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);
}
