package com.example.defridger.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.defridger.items.Ingredient;
import com.example.defridger.items.MeasuredIngredient;
import com.example.defridger.items.Recipe;

public class RecipeIngredientDbAdapter {
    public static final String TABLE_NAME = "RecipeIngredient";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private final Context context;

    public static final String KEY_ID = "_id";
    public static final String RECIPE_ID = "recipeID";
    public static final String INGREDIENT_ID = "ingredientID";
    public static final String AMOUNT = "amount";
    public static final String UNIT = "unit";

    public static final String[] FIELDS = new String[]{
            KEY_ID, RECIPE_ID, INGREDIENT_ID, AMOUNT, UNIT
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

    public RecipeIngredientDbAdapter(Context context) {
        this.context = context;
    }

    public RecipeIngredientDbAdapter open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (dbHelper != null)
            dbHelper.close();
    }

    public long insertRecipeIngredient(Recipe recipe, MeasuredIngredient measuredIngredient) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(RecipeIngredientDbAdapter.RECIPE_ID, recipe.id);
        initialValues.put(RecipeIngredientDbAdapter.INGREDIENT_ID, measuredIngredient.ingredientID);
        initialValues.put(RecipeIngredientDbAdapter.AMOUNT, measuredIngredient.amount);
        initialValues.put(RecipeIngredientDbAdapter.UNIT, measuredIngredient.unit);

        return db.insertOrThrow(TABLE_NAME, null, initialValues);
    }

    public boolean updateRecipeIngredient(int recipeID, int ingredientID, Recipe recipe, MeasuredIngredient measuredIngredient) {
        ContentValues newValues = new ContentValues();
        newValues.put(RecipeIngredientDbAdapter.RECIPE_ID, recipe.id);
        newValues.put(RecipeIngredientDbAdapter.INGREDIENT_ID, measuredIngredient.ingredientID);
        newValues.put(RecipeIngredientDbAdapter.AMOUNT, measuredIngredient.amount);
        newValues.put(RecipeIngredientDbAdapter.UNIT, measuredIngredient.unit);

        String[] selectionArgs = {String.valueOf(recipeID), String.valueOf(ingredientID)};
        return db.update(TABLE_NAME, newValues, RECIPE_ID + " = ? AND " + INGREDIENT_ID + " = ?", selectionArgs) > 0;
    }

    public boolean deleteRecipeIngredient(int recipeID, int ingredientID) {
        String[] selectionArgs = {String.valueOf(recipeID), String.valueOf(ingredientID)};
        return db.delete(TABLE_NAME, RECIPE_ID + " = ? AND " + INGREDIENT_ID + " = ?", selectionArgs) > 0;
    }

    public Cursor getAllRecipesIngredients() {
        return db.query(TABLE_NAME, FIELDS, null, null, null, null, KEY_ID);
    }

    public Cursor getRecipeIngredients(int recipeID) {
        String[] selectionArgs = {String.valueOf(recipeID)};
        return db.query(TABLE_NAME, FIELDS, RECIPE_ID + " = ?", selectionArgs, null, null, INGREDIENT_ID);
    }

    public static MeasuredIngredient getRecipeIngredientFromCursor(Cursor cursor) {
        MeasuredIngredient measuredIngredient = new MeasuredIngredient();
        measuredIngredient.ingredientID = cursor.getInt(cursor.getColumnIndexOrThrow(INGREDIENT_ID));
        measuredIngredient.amount = cursor.getFloat(cursor.getColumnIndexOrThrow(AMOUNT));
        measuredIngredient.unit = cursor.getString(cursor.getColumnIndexOrThrow(UNIT));

        return measuredIngredient;
    }
}
