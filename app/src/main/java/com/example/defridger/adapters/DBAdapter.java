package com.example.defridger.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.defridger.items.Ingredient;
import com.example.defridger.items.Recipe;

public class DBAdapter {
    public static final String DATABASE_NAME = "food_database.db";
    public static final int DATABASE_VERSION = 206;

    // Table definitions.
    private static final String CREATE_TABLE_INGREDIENT =
            "CREATE TABLE " + IngredientDbAdapter.TABLE_NAME + "( "
                    + IngredientDbAdapter.KEY_ID + " NUMBER(9) PRIMARY KEY, "
                    + IngredientDbAdapter.NAME + " VARCHAR2(100) NOT NULL, "
                    + IngredientDbAdapter.IMAGE + " VARBINARY(8000)"
                    + " );";

    private static final String CREATE_TABLE_RECIPE =
            "CREATE TABLE " + RecipeDbAdapter.TABLE_NAME + "( "
                    + RecipeDbAdapter.KEY_ID + " NUMBER(9) PRIMARY KEY, "
                    + RecipeDbAdapter.NAME + " VARCHAR2(100) NOT NULL, "
                    + RecipeDbAdapter.IMAGE + " VARBINARY(8000)"
                    + " );";

    private static final String CREATE_TABLE_USER_INGREDIENT =
            "CREATE TABLE " + UserIngredientDbAdapter.TABLE_NAME + "( "
                    + UserIngredientDbAdapter.KEY_ID + " NUMBER(9) PRIMARY KEY, "
                    + UserIngredientDbAdapter.INGREDIENT_ID + " NUMBER(9) NOT NULL UNIQUE, "
                    + UserIngredientDbAdapter.AMOUNT + " NUMBER(10, 2) NOT NULL, "
                    + UserIngredientDbAdapter.UNIT + " VARCHAR2(40) NOT NULL, "
                    + "FOREIGN KEY(" + UserIngredientDbAdapter.INGREDIENT_ID + ") REFERENCES " + IngredientDbAdapter.TABLE_NAME + "(" + IngredientDbAdapter.KEY_ID + ")"
                    + " );";

    private static final String CREATE_TABLE_RECIPE_INGREDIENT =
            "CREATE TABLE " + RecipeIngredientDbAdapter.TABLE_NAME + "( "
                    + RecipeIngredientDbAdapter.RECIPE_ID + " NUMBER(9) NOT NULL, "
                    + RecipeIngredientDbAdapter.INGREDIENT_ID + " NUMBER(9) NOT NULL, "
                    + RecipeIngredientDbAdapter.AMOUNT + " NUMBER(10, 2) NOT NULL, "
                    + RecipeIngredientDbAdapter.UNIT + " VARCHAR2(40) NOT NULL, "
                    + "CONSTRAINT " + RecipeIngredientDbAdapter.KEY_ID + " PRIMARY KEY (" + RecipeIngredientDbAdapter.RECIPE_ID + ", " + RecipeIngredientDbAdapter.INGREDIENT_ID + "), "
                    + "FOREIGN KEY(" + RecipeIngredientDbAdapter.RECIPE_ID + ") REFERENCES " + RecipeDbAdapter.TABLE_NAME + " (" + RecipeDbAdapter.KEY_ID + "), "
                    + "FOREIGN KEY(" + RecipeIngredientDbAdapter.INGREDIENT_ID + ") REFERENCES " + IngredientDbAdapter.TABLE_NAME + " (" + IngredientDbAdapter.KEY_ID + ")"
                    + " );";

    private static final String CREATE_TABLE_RECIPE_DETAILS =
            "CREATE TABLE " + RecipeDetailsDbAdapter.TABLE_NAME + "( "
                    + RecipeDetailsDbAdapter.KEY_ID + " NUMBER(9) PRIMARY KEY, "
                    + RecipeDetailsDbAdapter.RECIPE_ID + " NUMBER(9) UNIQUE, "
                    + RecipeDetailsDbAdapter.AUTHOR + " VARCHAR2(100) NOT NULL, "
                    + RecipeDetailsDbAdapter.SERVINGS + " NUMBER(3) NOT NULL, "
                    + RecipeDetailsDbAdapter.TIME + " NUMBER(3) NOT NULL, "
                    + RecipeDetailsDbAdapter.SOURCE_URL + " VARCHAR2(200) NOT NULL, "
                    + RecipeDetailsDbAdapter.IS_VEGAN + " NUMBER(1) NOT NULL, "
                    + RecipeDetailsDbAdapter.IS_VEGETARIAN + " NUMBER(1) NOT NULL, "
                    + "FOREIGN KEY(" + RecipeDetailsDbAdapter.RECIPE_ID + ") REFERENCES " + RecipeDbAdapter.TABLE_NAME + "(" + RecipeDbAdapter.KEY_ID + ")"
                    + " );";

    // Triggers.
    private static final String CREATE_DELETE_CORESPONDING_RECIPE_TRIGGER =
            "CREATE TRIGGER IF NOT EXISTS DeleteCorespodingRecipe"
                    + " AFTER DELETE ON " + RecipeDetailsDbAdapter.TABLE_NAME
                    + " FOR EACH ROW BEGIN"
                    + " DELETE FROM " + RecipeDbAdapter.TABLE_NAME
                    + " WHERE " + RecipeDbAdapter.KEY_ID + " = OLD." + RecipeDetailsDbAdapter.RECIPE_ID + ";"
                    + " END;";

    private static final String CREATE_DELETE_CORESPONDING_RECIPE_DETAILS_TRIGGER =
            "CREATE TRIGGER IF NOT EXISTS DeleteCorespodingRecipeDetails"
                    + " AFTER DELETE ON " + RecipeDbAdapter.TABLE_NAME
                    + " FOR EACH ROW BEGIN"
                    + " DELETE FROM " + RecipeDetailsDbAdapter.TABLE_NAME
                    + " WHERE " + RecipeDetailsDbAdapter.RECIPE_ID + " = OLD." + RecipeDbAdapter.KEY_ID + ";"
                    + " END;";

    private final Context context;
    private DatabaseHelper dbHelper;
    SQLiteDatabase db;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_INGREDIENT);
            db.execSQL(CREATE_TABLE_RECIPE);
            db.execSQL(CREATE_TABLE_USER_INGREDIENT);
            db.execSQL(CREATE_TABLE_RECIPE_INGREDIENT);
            db.execSQL(CREATE_TABLE_RECIPE_DETAILS);

            db.execSQL(CREATE_DELETE_CORESPONDING_RECIPE_TRIGGER);
            db.execSQL(CREATE_DELETE_CORESPONDING_RECIPE_DETAILS_TRIGGER);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Updating database will remove all of the data.
            db.execSQL("DROP TABLE IF EXISTS " + IngredientDbAdapter.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + RecipeDbAdapter.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + UserIngredientDbAdapter.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + RecipeIngredientDbAdapter.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + RecipeDetailsDbAdapter.TABLE_NAME);
            onCreate(db);
        }
    }

    public DBAdapter(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    public DBAdapter open() throws SQLException {
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (dbHelper != null)
            dbHelper.close();
    }

    public void upgrade() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        dbHelper.onUpgrade(db, 1, 0);
    }
}
