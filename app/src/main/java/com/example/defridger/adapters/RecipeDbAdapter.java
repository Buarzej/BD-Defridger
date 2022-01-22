package com.example.defridger.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.defridger.items.Ingredient;
import com.example.defridger.items.Recipe;

public class RecipeDbAdapter {
    public static final String TABLE_NAME = "Recipe";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private final Context context;

    public static final String KEY_ID = "_id";
    public static final String NAME = "name";
    public static final String IMAGE = "image";

    public static final String[] FIELDS = new String[]{
            KEY_ID, NAME, IMAGE
    };

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DBAdapter.DATABASE_NAME, null, DBAdapter.DATABASE_VERSION);
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            db.setForeignKeyConstraintsEnabled(true);
            super.onConfigure(db);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public RecipeDbAdapter(Context context) {
        this.context = context;
    }

    public RecipeDbAdapter open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (dbHelper != null)
            dbHelper.close();
    }

    public long insertRecipe(Recipe recipe) throws SQLException {
        ContentValues initialValues = new ContentValues();
        initialValues.put(RecipeDbAdapter.KEY_ID, recipe.id);
        initialValues.put(RecipeDbAdapter.NAME, recipe.name);
        initialValues.putNull(RecipeDbAdapter.IMAGE);

        return db.insertOrThrow(TABLE_NAME, null, initialValues);
    }

    public boolean updateRecipe(int id, Recipe recipe, byte[] image) {
        ContentValues newValues = new ContentValues();
        newValues.put(RecipeDbAdapter.KEY_ID, recipe.id);
        newValues.put(RecipeDbAdapter.NAME, recipe.name);
        newValues.put(RecipeDbAdapter.IMAGE, image);

        String[] selectionArgs = {String.valueOf(id)};
        return db.update(TABLE_NAME, newValues, KEY_ID + " = ?", selectionArgs) > 0;
    }

    public boolean deleteRecipe(int id) {
        String[] selectionArgs = {String.valueOf(id)};
        return db.delete(TABLE_NAME, KEY_ID + " = ?", selectionArgs) > 0;
    }

    public Cursor getRecipes() {
        return db.query(TABLE_NAME, FIELDS, null, null, null, null, NAME);
    }

    public static Recipe getRecipeFromCursor(Cursor cursor) {
        Recipe recipe = new Recipe();
        recipe.id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
        recipe.name = cursor.getString(cursor.getColumnIndexOrThrow(NAME));
        recipe.image = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE));

        return recipe;
    }
}
