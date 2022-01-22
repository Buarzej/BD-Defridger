package com.example.defridger.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.defridger.items.Ingredient;

public class IngredientDbAdapter {
    public static final String TABLE_NAME = "Ingredient";

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

    public IngredientDbAdapter(Context context) {
        this.context = context;
    }

    public IngredientDbAdapter open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (dbHelper != null)
            dbHelper.close();
    }

    public long insertIngredient(Ingredient ingredient) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(IngredientDbAdapter.KEY_ID, ingredient.id);
        initialValues.put(IngredientDbAdapter.NAME, ingredient.name);
        initialValues.putNull(IngredientDbAdapter.IMAGE);

        return db.insertOrThrow(TABLE_NAME, null, initialValues);
    }

    public boolean updateIngredient(int id, Ingredient ingredient, byte[] image) {
        ContentValues newValues = new ContentValues();
        newValues.put(IngredientDbAdapter.KEY_ID, ingredient.id);
        newValues.put(IngredientDbAdapter.NAME, ingredient.name);
        newValues.put(IngredientDbAdapter.IMAGE, image);

        String[] selectionArgs = {String.valueOf(id)};
        return db.update(TABLE_NAME, newValues, KEY_ID + " = ?", selectionArgs) > 0;
    }

    public boolean deleteIngredient(int id) {
        String[] selectionArgs = {String.valueOf(id)};
        return db.delete(TABLE_NAME, KEY_ID + " = ?", selectionArgs) > 0;
    }

    public Cursor getIngredients() {
        return db.query(TABLE_NAME, FIELDS, null, null, null, null, NAME);
    }

    public Ingredient getIngredientByID(int id) {
        String[] selectionArgs = {String.valueOf(id)};
        Cursor query = db.query(TABLE_NAME, FIELDS, KEY_ID + " = ?", selectionArgs, null, null, null);

        return getIngredientFromCursor(query);
    }

    public static Ingredient getIngredientFromCursor(Cursor cursor) {
        Ingredient ingredient = new Ingredient();
        ingredient.id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
        ingredient.name = cursor.getString(cursor.getColumnIndexOrThrow(NAME));
        ingredient.image = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE));

        return ingredient;
    }
}
