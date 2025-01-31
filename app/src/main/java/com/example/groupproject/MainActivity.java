package com.example.groupproject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView nicknameText, signUpOption, scoreTextView, timerTextView;
    private Button playButton, resetButton, exitButton;
    private SharedPreferences sharedPreferences;
    private int timeTaken = 0;  // Time in seconds
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private GridView gridView;
    private GridAdapter adapter;
    private Integer[] allImages = {
            R.drawable.image1, R.drawable.image2, R.drawable.image3, R.drawable.image4,
            R.drawable.image5, R.drawable.image6, R.drawable.image7, R.drawable.image8,
            R.drawable.image9, R.drawable.image10, R.drawable.image11, R.drawable.image12,
            R.drawable.image13, R.drawable.image14, R.drawable.image15, R.drawable.image16,
            R.drawable.image17, R.drawable.image18, R.drawable.image19, R.drawable.image20,
            R.drawable.image21, R.drawable.image22
    };

    private Integer[] selectedImages;
    private int[] revealed; // Tracks revealed cards (0 for hidden, 1 for revealed)
    private int firstSelected = -1, secondSelected = -1;
    private int matchedPairs = 0;
    private boolean isChecking = false;
    private Handler handler = new Handler();

    private int pairsMatched = 0;
    private int incorrectGuesses = 0;
    private int score = 0;  // Initial score

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI Components
        nicknameText = findViewById(R.id.nickname_text);
        signUpOption = findViewById(R.id.sign_up_option);
        timerTextView = findViewById(R.id.timerTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        gridView = findViewById(R.id.gridView);
        playButton = findViewById(R.id.playButton);
        resetButton = findViewById(R.id.resetButton);
        exitButton = findViewById(R.id.exitButton);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("userPreferences", MODE_PRIVATE);

        gridView.setEnabled(false); // Disable clicks at start
        resetButton.setEnabled(false); // Disable the Reset button initially

        // Check user login status
        checkLoginStatus();

        // Get the nickname from the intent
        Intent intent = getIntent();
        String nickname = intent.getStringExtra("nickname");

        if (nickname != null && !nickname.isEmpty()) {
            nicknameText.setText(nickname); // Update the TextView
            signUpOption.setText("Log out");
        } else {
            nicknameText.setText("Guest"); // Fallback if no nickname is passed
        }

        signUpOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nicknameText.getText().toString().equals("Guest")) {
                    // User is not logged in, so allow them to sign up
                    Intent intentToRegister = new Intent(MainActivity.this, RegisterActivity.class);
                    startActivity(intentToRegister);
                    finish();
                } else {
                    // User is logged in, so allow them to log out
                    logoutUser();
                }
            }
        });


        Button highScoresButton = findViewById(R.id.highScoresButton);
        highScoresButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHighScores();
            }
        });

        // Set the listener for the reset button
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gridView.setEnabled(true);  // Enable clicks when Play is pressed
                resetButton.setEnabled(true); // Enable the Reset button

                setupGame();  // Set up the game and start the timer when the play button is clicked
                startTimer();  // Start the timer when the game begins

                // Show all cards briefly at the start
                showAllCardsTemporarily();
            }
        });

        // Set the listener for the reset button
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetGame();
            }
        });

        // Set the listener for the exit button
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        setupGame(); // Initialize the game

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handleCardClick(position);
            }
        });
    }

    private void checkLoginStatus() {
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String nickname = sharedPreferences.getString("nickname", "Guest");

        if (isLoggedIn && !nickname.isEmpty()) {
            nicknameText.setText(nickname);
            nicknameText.setClickable(true);
            nicknameText.setFocusable(true);
            nicknameText.setOnClickListener(v -> openNicknameActivity());
            signUpOption.setText("Log Out");
        } else {
            setGuestMode();
        }

        signUpOption.setOnClickListener(v -> {
            if (isLoggedIn) {
                logoutUser();
            } else {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
                finish();
            }
        });
    }

    private void setGuestMode() {
        nicknameText.setText("Guest");
        nicknameText.setClickable(false);
        nicknameText.setFocusable(false);
        signUpOption.setText("Sign Up");
    }

    private void openNicknameActivity() {
        Intent intent = new Intent(MainActivity.this, NicknameActivity.class);
        startActivity(intent);
    }

    private void logoutUser() {
        // Set the nickname to "Guest" and update the UI accordingly
        nicknameText.setText("Guest");
        signUpOption.setText("Sign Up"); // Change the button text back to "Sign Up"
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String nickname = data.getStringExtra("nickname");
            if (nickname != null) {
                nicknameText.setText(nickname);
                signUpOption.setText("Log Out"); // Change button to log out when logged in
            }
        }
    }

    private void showAllCardsTemporarily() {
        gridView.setEnabled(false);

        for (int i = 0; i < revealed.length; i++) {
            revealed[i] = 1; // Show all cards
            View cardView = gridView.getChildAt(i);
            if (cardView != null) {
                flipCard(gridView.getChildAt(i), i);
            }
        }
        adapter.notifyDataSetChanged();

        new Handler().postDelayed(() -> {
            for (int i = 0; i < revealed.length; i++) {
                revealed[i] = 0; // Hide all cards again
                View cardView = gridView.getChildAt(i);
                if (cardView != null) {
                    flipCard(gridView.getChildAt(i), i);
                }
            }
            adapter.notifyDataSetChanged();
        }, 2000);

        gridView.setEnabled(true);
    }

    private void setupGame() {
        // Initialize game variables
        pairsMatched = 0;
        incorrectGuesses = 0;
        timeTaken = 0;
        score = 0;

        List<Integer> imageList = new ArrayList<>(Arrays.asList(allImages));

        // Shuffle all images and pick 8 random ones
        Collections.shuffle(imageList);
        List<Integer> chosenImages = imageList.subList(0, 8);

        // Duplicate images to create pairs
        List<Integer> gameImages = new ArrayList<>();
        for (Integer img : chosenImages) {
            gameImages.add(img);
            gameImages.add(img);
        }

        // Shuffle the final list of 16 images
        Collections.shuffle(gameImages);
        selectedImages = gameImages.toArray(new Integer[0]);

        revealed = new int[16]; // Initialize revealed states (all hidden)

        adapter = new GridAdapter(this, selectedImages, revealed);
        gridView.setAdapter(adapter);
    }

    private void handleCardClick(int position) {
        if (isChecking || revealed[position] == 1) return; // Ignore if checking or already revealed

        View cardView = gridView.getChildAt(position);
        if (cardView == null) return; // Prevent crashes if grid hasn't loaded fully

        flipCard(cardView, position); // Perform flip animation

        if (firstSelected == -1) {
            firstSelected = position;
            revealed[firstSelected] = 1;
            adapter.notifyDataSetChanged();
        } else if (secondSelected == -1 && position != firstSelected) {
            secondSelected = position;
            revealed[secondSelected] = 1;
            adapter.notifyDataSetChanged();

            isChecking = true;
            handler.postDelayed(this::checkForMatch, 1000); // Delay checking for match
        }
    }

    private void checkForMatch() {
        if (selectedImages[firstSelected].equals(selectedImages[secondSelected])) {
            matchedPairs++;
            pairsMatched++;
            updateScoreDisplay();  // Update the score when a match happens

            // Fade out animation for matched cards
            gridView.getChildAt(firstSelected).animate().alpha(0).setDuration(500);
            gridView.getChildAt(secondSelected).animate().alpha(0).setDuration(500);

            if (matchedPairs == 8) {
                Toast.makeText(this, "You won!", Toast.LENGTH_SHORT).show();
                stopTimer();  // Stop the timer when the game is won
            }
        } else {
            incorrectGuesses++;
            updateScoreDisplay();  // Update the score when an incorrect guess happens

            // Flip back animation for non-matching cards
            flipBack(gridView.getChildAt(firstSelected), firstSelected);
            flipBack(gridView.getChildAt(secondSelected), secondSelected);
        }

        firstSelected = -1;
        secondSelected = -1;
        isChecking = false;
    }

    private void resetGame() {
        // Stop the timer and reset everything
        stopTimer(); // This stops the timer and saves the score

        // Reset the score
        score = 0;
        scoreTextView.setText("Score: " + score);

        // Reset the timer (time is set to 0:00)
        int minutes = 0;
        int seconds = 0;

        // Format the time in "Time: MM:SS" format
        String timeString = String.format("Time: %02d:%02d", minutes, seconds);
        timerTextView.setText(timeString); // Update the timer TextView with the formatted time

        // Show all cards and reset their state to visible
        for (int i = 0; i < revealed.length; i++) {
            revealed[i] = 1; // Set all cards as revealed (visible)
            View cardView = gridView.getChildAt(i);
            if (cardView != null) {
                // Show the card by resetting its scale and alpha
                cardView.setScaleX(1f);
                cardView.setScaleY(1f);
                cardView.setAlpha(1f);
                cardView.setVisibility(View.VISIBLE);  // Ensure cards are visible
            }
        }

        // Update the GridView to reflect the reset state
        adapter.notifyDataSetChanged(); // This ensures the cards are shown as reset

        // Re-enable the play button (in case it was disabled during the game)
        playButton.setEnabled(true);
        playButton.setVisibility(View.VISIBLE);

        showAllCardsTemporarily();
    }

    // Method to start the timer and update it
    private void startTimer() {
        // Disable the Play button when timer starts
        Button playButton = findViewById(R.id.playButton);
        playButton.setEnabled(false);  // Disable Play button

        // Initialize and start the timer when the play button is clicked
        timeTaken = 0;  // Reset timer at the start
        updateTimerDisplay();

        // Runnable to update the timer every second
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timeTaken++;  // Increment the time
                updateTimerDisplay();  // Update the timer display

                // Repeat the task every second (1000 milliseconds)
                timerHandler.postDelayed(this, 1000);
            }
        };

        // Start the timer immediately
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void updateTimerDisplay() {
        int minutes = timeTaken / 60;
        int seconds = timeTaken % 60;

        // Format the time to always show two digits for minutes and seconds
        String timeString = String.format("Time: %02d:%02d", minutes, seconds);
        timerTextView.setText(timeString);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);

        // Save score in database
        DatabaseHelper dbHelper = new DatabaseHelper(this);

        // Get the nickname ID (You need to retrieve this based on the logged-in user)
        int nicknameId = getNicknameIdFromDatabase(); // Implement this method

        if (nicknameId != -1) {
            boolean success = dbHelper.setScore(nicknameId, score);
            if (success) {
                Toast.makeText(this, "Score saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save score.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int getNicknameIdFromDatabase() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT id FROM nicknames WHERE nickname = ?",
                new String[]{nicknameText.getText().toString()}); // Get nickname from input field

        if (cursor.moveToFirst()) {
            int nicknameId = cursor.getInt(0);
            cursor.close();
            return nicknameId;
        } else {
            cursor.close();
            return -1; // Nickname not found
        }
    }

    private void showHighScores() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT nickname, score FROM nicknames ORDER BY score DESC", null);

        StringBuilder scoreText = new StringBuilder();
        while (cursor.moveToNext()) {
            String nickname = cursor.getString(0);
            int score = cursor.getInt(1);
            scoreText.append(nickname).append(": ").append(score).append("\n");
        }
        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("Leaderboard")
                .setMessage(scoreText.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // Method to calculate the score
    private void calculateScore() {
        int pointsPerPair = 50;
        int penaltyPerIncorrectGuess = 5;
        int bonusForSpeed = (timeTaken <= 30) ? 20 : 0;  // Bonus if completed in under 30 seconds

        score = (pairsMatched * pointsPerPair) - (incorrectGuesses * penaltyPerIncorrectGuess) + bonusForSpeed;

        // Optional: Deduct time penalty
        int timePenalty = timeTaken * 1;  // Deduct 1 point per second
        score = Math.max(0, score - timePenalty);  // Ensure score doesn't go below 0
    }

    // Method to update the score display
    private void updateScoreDisplay() {
        calculateScore();
        // Update the score TextView with the current score
        scoreTextView.setText("Score: " + score);
    }

    private void flipCard(View cardView, int position) {
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 0f);
        ObjectAnimator flipIn = ObjectAnimator.ofFloat(cardView, "scaleX", 0f, 1f);

        flipOut.setDuration(200);
        flipIn.setDuration(200);

        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Change the image once the first half of the flip is done
                adapter.notifyDataSetChanged();
                flipIn.start(); // Start the second half of the flip
            }
        });

        flipOut.start(); // Start the first half of the flip
    }

    private void flipBack(View cardView, int position) {
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 0f);
        ObjectAnimator flipIn = ObjectAnimator.ofFloat(cardView, "scaleX", 0f, 1f);

        flipOut.setDuration(200);
        flipIn.setDuration(200);

        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset the card to hidden state after first half of flip
                revealed[position] = 0;
                adapter.notifyDataSetChanged();
                flipIn.start(); // Start the second half of the flip
            }
        });

        flipOut.start(); // Start the first half of the flip
    }
}
