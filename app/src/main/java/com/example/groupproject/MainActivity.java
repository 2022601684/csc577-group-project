package com.example.groupproject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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

        gridView = findViewById(R.id.gridView);
        // Find the Button
        Button playButton = findViewById(R.id.playButton);

        // Set the listener for the reset button
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupGame();
            }
        });
        setupGame(); // Initialize the game

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handleCardClick(position);
            }
        });

        // Show all cards briefly at the start
        showAllCardsTemporarily();

        // Find the Button
        Button resetButton = findViewById(R.id.resetButton);

        // Set the listener for the reset button
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAllCardsTemporarily();

                resetGame();
            }
        });
    }

    private void showAllCardsTemporarily() {
        for (int i = 0; i < revealed.length; i++) {
            revealed[i] = 1; // Show all cards
            View cardView = gridView.getChildAt(i);
            if (cardView != null) {
                cardView.setScaleX(0f);
                cardView.setScaleY(0f);
                cardView.animate().scaleX(1f).scaleY(1f).setDuration(1000);
            }
        }
        adapter.notifyDataSetChanged();

        new Handler().postDelayed(() -> {
            for (int i = 0; i < revealed.length; i++) {
                revealed[i] = 0;
            }
            adapter.notifyDataSetChanged();
        }, 2000);
    }

    private void setupGame() {
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

            // Fade out animation for matched cards
            gridView.getChildAt(firstSelected).animate().alpha(0).setDuration(500);
            gridView.getChildAt(secondSelected).animate().alpha(0).setDuration(500);

            if (matchedPairs == 8) {
                Toast.makeText(this, "You won!", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Flip back animation for non-matching cards
            flipBack(gridView.getChildAt(firstSelected), firstSelected);
            flipBack(gridView.getChildAt(secondSelected), secondSelected);
        }

        firstSelected = -1;
        secondSelected = -1;
        isChecking = false;
    }

    private void resetGame() {
        matchedPairs = 0;
        firstSelected = -1;
        secondSelected = -1;
        setupGame();
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
