package com.example.defridger.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defridger.R;

public class UserIngredientListAdapter extends RecyclerView.Adapter<UserIngredientListAdapter.ViewHolder> {

    // Because RecyclerView.Adapter in its current form doesn't natively
    // support cursors, we wrap a CursorAdapter that will do all the job
    // for us.
    final CursorAdapter cursorAdapter;
    final Context context;

    public UserIngredientListAdapter(Context context, Cursor c) {
        this.context = context;
        this.cursorAdapter = new CursorAdapter(context, c, 0) {

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context)
                        .inflate(R.layout.ingredient_view_holder, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ImageView ingredientImage = (ImageView) view.findViewById(R.id.recipeImage);
                TextView ingredientName = (TextView) view.findViewById(R.id.recipeName);
                ImageButton ingredientClearButton = (ImageButton) view.findViewById(R.id.ingredientClearButton);

                String name = cursor.getString(cursor.getColumnIndexOrThrow(IngredientDbAdapter.NAME));
                byte[] image = cursor.getBlob(cursor.getColumnIndexOrThrow(IngredientDbAdapter.IMAGE));

                Bitmap bmp;
                if (image != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    bmp = BitmapFactory.decodeByteArray(image, 0, image.length, options);
                } else {
                    bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.unknown_ingredient);
                }

                ingredientName.setText(name);
                ingredientImage.setImageBitmap(bmp);

                final int position = cursor.getPosition();
                ingredientClearButton.setOnClickListener(view1 -> {
                    IngredientDbAdapter ingredientDbAdapter = new IngredientDbAdapter(context);
                    ingredientDbAdapter.open();
                    UserIngredientDbAdapter userIngredientDbAdapter = new UserIngredientDbAdapter(context);
                    userIngredientDbAdapter.open();

                    cursor.moveToPosition(position);
                    int userIngredientID = cursor.getInt(cursor.getColumnIndexOrThrow(UserIngredientDbAdapter.KEY_ID));
                    int ingredientID = cursor.getInt(cursor.getColumnIndexOrThrow(IngredientDbAdapter.KEY_ID));
                    userIngredientDbAdapter.deleteUserIngredient(userIngredientID);
                    try {
                        ingredientDbAdapter.deleteIngredient(ingredientID);
                    } catch (SQLiteConstraintException e) {
                        // Ingredient still present in at least one of the database recipes.
                    }

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    sharedPreferences.edit().putBoolean("hasListChanged", true).apply();

                    userIngredientDbAdapter.close();
                    ingredientDbAdapter.close();
                    cursor.requery();
                    UserIngredientListAdapter.this.notifyDataSetChanged();
                });
            }
        };
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ingredientImage;
        final TextView ingredientName;
        final ImageButton ingredientClearButton;

        public ViewHolder(View itemView) {
            super(itemView);

            ingredientImage = (ImageView) itemView.findViewById(R.id.recipeImage);
            ingredientName = (TextView) itemView.findViewById(R.id.recipeName);
            ingredientClearButton = (ImageButton) itemView.findViewById(R.id.ingredientClearButton);
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
