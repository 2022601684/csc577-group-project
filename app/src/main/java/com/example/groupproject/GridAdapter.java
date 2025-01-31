package com.example.groupproject;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class GridAdapter extends BaseAdapter {
    private Context context;
    private Integer[] images; // Array of card images
    private int[] revealed;   // Array tracking revealed cards (1 = revealed, 0 = hidden)

    public GridAdapter(Context context, Integer[] images, int[] revealed) {
        this.context = context;
        this.images = images;
        this.revealed = revealed;
    }

    @Override
    public int getCount() {
        return images.length;
    }

    @Override
    public Object getItem(int position) {
        return images[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        }

        ImageView imageView = convertView.findViewById(R.id.cardImage);

        if (revealed[position] == 1) {
            imageView.setImageResource(images[position]); // Show image if revealed
        } else {
            imageView.setImageResource(R.drawable.hidden_card); // Hide card
        }

        return convertView;
    }
}
