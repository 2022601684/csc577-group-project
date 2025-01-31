package com.example.groupproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegisterActivity extends AppCompatActivity {
    EditText editTextUsername, editTextPassword;
    Button btnRegister;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(view -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (!username.isEmpty() && !password.isEmpty()) {
                if (dbHelper.registerUser(username, password)) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

                    // Navigate to NicknameActivity after successful registration
                    Intent intent = new Intent(this, NicknameActivity.class);
                    intent.putExtra("username", username); // Passing username for reference if needed
                    startActivity(intent);
                    finish(); // Close RegisterActivity

                } else {
                    Toast.makeText(this, "Username already exists!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void goToLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}
