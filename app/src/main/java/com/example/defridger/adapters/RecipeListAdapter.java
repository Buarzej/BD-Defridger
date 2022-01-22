package com.example.defridger.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defridger.R;

public class RecipeListAdapter extends RecyclerView.Adapter<RecipeListAdapter.ViewHolder> {

    // Because RecyclerView.Adapter in its current form doesn't natively
    // support cursors, we wrap a CursorAdapter that will do all the job
    // for us.
    CursorAdapter cursorAdapter;
    Context context;

    public RecipeListAdapter(Context context, Cursor c) {
        this.context = context;
        this.cursorAdapter = new CursorAdapter(context, c, 0) {

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context)
                        .inflate(R.layout.recipe_view_holder, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ImageView recipeImage = (ImageView) view.findViewById(R.id.recipeImage);
                TextView recipeName = (TextView) view.findViewById(R.id.recipeName);
                TextView recipeTime = (TextView) view.findViewById(R.id.timeValue);
                TextView matchingIngredients = (TextView) view.findViewById(R.id.matchingValue);

                String name = cursor.getString(cursor.getColumnIndexOrThrow(RecipeDbAdapter.NAME));
                int time = cursor.getInt(cursor.getColumnIndexOrThrow(RecipeDetailsDbAdapter.TIME));
                byte[] image = cursor.getBlob(cursor.getColumnIndexOrThrow(RecipeDbAdapter.IMAGE));
                String url = cursor.getString(cursor.getColumnIndexOrThrow(RecipeDetailsDbAdapter.SOURCE_URL));
                int matching = cursor.getInt(cursor.getColumnIndexOrThrow("matchingIngredients"));

                recipeName.setText(name);
                recipeTime.setText(context.getResources().getString(R.string.time_format, time));
                matchingIngredients.setText(String.valueOf(matching));

                Bitmap bmp;
                if (image != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    bmp = BitmapFactory.decodeByteArray(image, 0, image.length, options);
                } else {
                    bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.unknown_ingredient);
                }
                recipeImage.setImageBitmap(bmp);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        context.startActivity(browserIntent);
                    }
                });
            }
        };
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView recipeImage;
        TextView recipeName;
        TextView recipeTime;
        TextView matchingIngredients;

        public ViewHolder(View itemView) {
            super(itemView);

            recipeImage = (ImageView) itemView.findViewById(R.id.recipeImage);
            recipeName = (TextView) itemView.findViewById(R.id.recipeName);
            recipeTime = (TextView) itemView.findViewById(R.id.timeValue);
            matchingIngredients = (TextView) itemView.findViewById(R.id.matchingValue);
        }
    }

    @Override
    public int getItemCount() {
        return cursorAdapter.getCount();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Pass the binding operation to cursor loader.
        cursorAdapter.getCursor().moveToPosition(position);
        cursorAdapter.bindView(holder.itemView, context, cursorAdapter.getCursor());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Pass the inflater job to the cursor adapter.
        View v = cursorAdapter.newView(context, cursorAdapter.getCursor(), parent);
        return new ViewHolder(v);
    }
}
