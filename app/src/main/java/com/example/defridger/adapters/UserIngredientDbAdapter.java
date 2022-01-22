package com.example.defridger.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.defridger.items.Ingredient;
import com.example.defridger.items.MeasuredIngredient;

import java.util.ArrayList;

public class UserIngredientDbAdapter {
    public static final String TABLE_NAME = "UserIngredient";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private final Context context;

    public static final String KEY_ID = "_id";
    public static final String INGREDIENT_ID = "ingredientID";
    public static final String AMOUNT = "amount";
    public static final String UNIT = "unit";

    public static final String[] FIELDS = new String[]{
            KEY_ID, INGREDIENT_ID, AMOUNT, UNIT
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

    public UserIngredientDbAdapter(Context context) {
        this.context = context;
    }

    public UserIngredientDbAdapter open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (dbHelper != null)
            dbHelper.close();
    }

    public long insertUserIngredient(MeasuredIngredient measuredIngredient) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(UserIngredientDbAdapter.KEY_ID, measuredIngredient.ingredientID);
        initialValues.put(UserIngredientDbAdapter.INGREDIENT_ID, measuredIngredient.ingredientID);
        initialValues.put(UserIngredientDbAdapter.AMOUNT, measuredIngredient.amount);
        initialValues.put(UserIngredientDbAdapter.UNIT, measuredIngredient.unit);

        return db.insertOrThrow(TABLE_NAME, null, initialValues);
    }

    public boolean updateUserIngredient(int id, MeasuredIngredient measuredIngredient) {
        ContentValues newValues = new ContentValues();
        newValues.put(UserIngredientDbAdapter.KEY_ID, measuredIngredient.ingredientID);
        newValues.put(UserIngredientDbAdapter.INGREDIENT_ID, measuredIngredient.ingredientID);
        newValues.put(UserIngredientDbAdapter.AMOUNT, measuredIngredient.amount);
        newValues.put(UserIngredientDbAdapter.UNIT, measuredIngredient.unit);

        String[] selectionArgs = {String.valueOf(id)};
        return db.update(TABLE_NAME, newValues, KEY_ID + " = ?", selectionArgs) > 0;
    }

    public boolean deleteUserIngredient(int id) {
        String[] selectionArgs = {String.valueOf(id)};
        return db.delete(TABLE_NAME, KEY_ID + " = ?", selectionArgs) > 0;
    }

    public Cursor getUserIngredients() {
        return db.rawQuery("SELECT * FROM " + UserIngredientDbAdapter.TABLE_NAME + " U JOIN " + IngredientDbAdapter.TABLE_NAME + " I ON U."
                        + UserIngredientDbAdapter.INGREDIENT_ID + " = I." + IngredientDbAdapter.KEY_ID + " ORDER BY I." + IngredientDbAdapter.NAME + ";",
                null);
    }

    public ArrayList<String> getUserIngredientsList() {
        Cursor cursor = db.rawQuery("SELECT " + IngredientDbAdapter.NAME + " FROM " + UserIngredientDbAdapter.TABLE_NAME + " U JOIN " + IngredientDbAdapter.TABLE_NAME + " I ON U."
                        + UserIngredientDbAdapter.INGREDIENT_ID + " = I." + IngredientDbAdapter.KEY_ID + " ORDER BY I." + IngredientDbAdapter.NAME + ";",
                null);

        ArrayList<String> ingredientNames = new ArrayList<>();
        cursor.moveToFirst();
        do {
            ingredientNames.add(cursor.getString(cursor.getColumnIndexOrThrow(IngredientDbAdapter.NAME)));
        } while (cursor.moveToNext());

        cursor.close();
        return ingredientNames;
    }

    public static MeasuredIngredient getUserIngredientFromCursor(Cursor cursor) {
        MeasuredIngredient measuredIngredient = new MeasuredIngredient();
        measuredIngredient.ingredientID = cursor.getInt(cursor.getColumnIndexOrThrow(INGREDIENT_ID));
        measuredIngredient.amount = cursor.getFloat(cursor.getColumnIndexOrThrow(AMOUNT));
        measuredIngredient.unit = cursor.getString(cursor.getColumnIndexOrThrow(UNIT));

        return measuredIngredient;
    }
}
