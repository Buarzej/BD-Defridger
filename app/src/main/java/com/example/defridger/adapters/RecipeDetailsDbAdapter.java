package com.example.defridger.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.defridger.items.RecipeDetails;

public class RecipeDetailsDbAdapter {
    public static final String TABLE_NAME = "RecipeDetails";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private final Context context;

    public static final String KEY_ID = "_id";
    public static final String RECIPE_ID = "recipeID";
    public static final String AUTHOR = "author";
    public static final String SERVINGS = "servings";
    public static final String TIME = "time";
    public static final String SOURCE_URL = "sourceUrl";
    public static final String IS_VEGAN = "isVegan";
    public static final String IS_VEGETARIAN = "isVegetarian";

    public static final String[] FIELDS = new String[]{
            KEY_ID, RECIPE_ID, AUTHOR, SERVINGS, TIME, SOURCE_URL, IS_VEGAN, IS_VEGETARIAN
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

    public RecipeDetailsDbAdapter(Context context) {
        this.context = context;
    }

    public RecipeDetailsDbAdapter open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (dbHelper != null)
            dbHelper.close();
    }

    public long insertRecipeDetails(RecipeDetails recipeDetails) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(RecipeDetailsDbAdapter.KEY_ID, recipeDetails.id);
        initialValues.put(RecipeDetailsDbAdapter.RECIPE_ID, recipeDetails.recipeID);
        initialValues.put(RecipeDetailsDbAdapter.AUTHOR, recipeDetails.author);
        initialValues.put(RecipeDetailsDbAdapter.SERVINGS, recipeDetails.servings);
        initialValues.put(RecipeDetailsDbAdapter.TIME, recipeDetails.time);
        initialValues.put(RecipeDetailsDbAdapter.SOURCE_URL, recipeDetails.sourceURL);
        initialValues.put(RecipeDetailsDbAdapter.IS_VEGAN, recipeDetails.isVegan ? 1 : 0);
        initialValues.put(RecipeDetailsDbAdapter.IS_VEGETARIAN, recipeDetails.isVegetarian ? 1 : 0);

        return db.insertOrThrow(TABLE_NAME, null, initialValues);
    }

    public boolean updateRecipeDetails(int id, RecipeDetails recipeDetails) {
        ContentValues newValues = new ContentValues();
        newValues.put(RecipeDetailsDbAdapter.KEY_ID, recipeDetails.id);
        newValues.put(RecipeDetailsDbAdapter.RECIPE_ID, recipeDetails.recipeID);
        newValues.put(RecipeDetailsDbAdapter.AUTHOR, recipeDetails.author);
        newValues.put(RecipeDetailsDbAdapter.SERVINGS, recipeDetails.servings);
        newValues.put(RecipeDetailsDbAdapter.TIME, recipeDetails.time);
        newValues.put(RecipeDetailsDbAdapter.SOURCE_URL, recipeDetails.sourceURL);
        newValues.put(RecipeDetailsDbAdapter.IS_VEGAN, recipeDetails.isVegan ? 1 : 0);
        newValues.put(RecipeDetailsDbAdapter.IS_VEGETARIAN, recipeDetails.isVegetarian ? 1 : 0);

        String[] selectionArgs = {String.valueOf(id)};
        return db.update(TABLE_NAME, newValues, KEY_ID + " = ?", selectionArgs) > 0;
    }

    public boolean deleteRecipeDetails(int id) {
        String[] selectionArgs = {String.valueOf(id)};
        return db.delete(TABLE_NAME, KEY_ID + " = ?", selectionArgs) > 0;
    }

    public Cursor getAllRecipesDetails() {
        String matchingSubquery = "SELECT " + RecipeIngredientDbAdapter.RECIPE_ID + " AS rID, COUNT(U_ING."
                + UserIngredientDbAdapter.INGREDIENT_ID + ") AS matchingIngredients FROM " + RecipeIngredientDbAdapter.TABLE_NAME
                + " R_ING LEFT JOIN " + UserIngredientDbAdapter.TABLE_NAME + " U_ING ON R_ING." + RecipeIngredientDbAdapter.INGREDIENT_ID
                + " = U_ING." + UserIngredientDbAdapter.INGREDIENT_ID + " GROUP BY R_ING." + RecipeIngredientDbAdapter.RECIPE_ID;
        return db.rawQuery("SELECT * FROM " + RecipeDbAdapter.TABLE_NAME + " R JOIN " + RecipeDetailsDbAdapter.TABLE_NAME + " D ON R."
                        + RecipeDbAdapter.KEY_ID + " = D." + RecipeDetailsDbAdapter.RECIPE_ID + " JOIN (" + matchingSubquery
                        + ") ON R." + RecipeDbAdapter.KEY_ID + " = rID WHERE matchingIngredients > 0 ORDER BY matchingIngredients DESC, R." + RecipeDbAdapter.NAME + ";",
                null);
    }

    public Cursor getRecipeDetails(int detailsID) {
        String[] selectionArgs = {String.valueOf(detailsID)};
        return db.query(TABLE_NAME, FIELDS, KEY_ID + " = ?", selectionArgs, null, null, null);
    }
}
