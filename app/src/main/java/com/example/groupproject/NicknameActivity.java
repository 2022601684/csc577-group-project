package com.example.groupproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class NicknameActivity extends AppCompatActivity {
    DatabaseHelper dbHelper;
    EditText editTextNickname;
    Button btnAdd, btnDelete, btnUpdate, btnGoToMain;
    ListView listViewNicknames;
    ArrayAdapter<String> adapter;
    ArrayList<String> nicknamesList;
    int selectedNicknameId = -1;
    String currentUsername; // Store the username of the logged-in user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nickname);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        editTextNickname = findViewById(R.id.editTextNickname);
        btnAdd = findViewById(R.id.btnAdd);
        btnDelete = findViewById(R.id.btnDelete);
        btnUpdate = findViewById(R.id.btnUpdate);
        listViewNicknames = findViewById(R.id.listViewNicknames);
        btnGoToMain = findViewById(R.id.btnGoToMain);

        // Assuming you are getting the username from the logged-in session or intent
        currentUsername = getIntent().getStringExtra("username"); // Pass username from LoginActivity

        // Get user ID based on the username
        int userId = dbHelper.getUserId(currentUsername);

        // Check if userId is valid
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        loadNicknames(userId); // Pass the userId to load nicknames

        btnAdd.setOnClickListener(view -> {
            String nickname = editTextNickname.getText().toString().trim();
            if (!nickname.isEmpty()) {
                if (dbHelper.addNickname(nickname, userId)) {  // Pass userId when adding nickname
                    Toast.makeText(this, "Nickname added!", Toast.LENGTH_SHORT).show();
                    loadNicknames(userId); // Refresh the nickname list
                } else {
                    Toast.makeText(this, "Error adding nickname!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnUpdate.setOnClickListener(view -> {
            if (selectedNicknameId != -1) {
                String newNickname = editTextNickname.getText().toString().trim();
                if (!newNickname.isEmpty()) {
                    if (dbHelper.updateNickname(selectedNicknameId, newNickname)) {
                        Toast.makeText(this, "Nickname updated!", Toast.LENGTH_SHORT).show();
                        loadNicknames(userId); // Refresh the nickname list
                    } else {
                        Toast.makeText(this, "Error updating nickname!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnDelete.setOnClickListener(view -> {
            if (selectedNicknameId != -1) {
                if (dbHelper.deleteNickname(selectedNicknameId)) {
                    Toast.makeText(this, "Nickname deleted!", Toast.LENGTH_SHORT).show();
                    loadNicknames(userId); // Refresh the nickname list
                } else {
                    Toast.makeText(this, "Error deleting nickname!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        listViewNicknames.setOnItemClickListener((parent, view, position, id) -> {
            selectedNicknameId = position + 1; // Assuming ID starts from 1
            editTextNickname.setText(nicknamesList.get(position));
        });

        btnGoToMain.setOnClickListener(view -> {
            String selectedNickname = editTextNickname.getText().toString().trim();
            if (!selectedNickname.isEmpty()) {
                Intent intent = new Intent(NicknameActivity.this, MainActivity.class);
                intent.putExtra("nickname", selectedNickname); // Pass the nickname
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadNicknames(int userId) {
        Cursor cursor = dbHelper.getAllNicknames(userId);  // Pass userId to load nicknames

        if (cursor == null) {
            Log.e("NicknameActivity", "Cursor is null, no data fetched");
            return;
        }

        nicknamesList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                String nickname = cursor.getString(cursor.getColumnIndex("nickname"));
                Log.d("NicknameActivity", "Loaded nickname: " + nickname);
                nicknamesList.add(nickname);
            } while (cursor.moveToNext());

            cursor.close();
        } else {
            Log.e("NicknameActivity", "No nicknames found in the database.");
        }

        if (nicknamesList.isEmpty()) {
            Toast.makeText(this, "No nicknames available", Toast.LENGTH_SHORT).show();
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nicknamesList);
        listViewNicknames.setAdapter(adapter);
    }
}