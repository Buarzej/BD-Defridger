package com.example.defridger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.defridger.adapters.DBAdapter;
import com.example.defridger.adapters.UserIngredientListAdapter;
import com.example.defridger.adapters.IngredientDbAdapter;
import com.example.defridger.adapters.UserIngredientDbAdapter;
import com.example.defridger.items.Ingredient;
import com.example.defridger.items.MeasuredIngredient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.spoonacular.IngredientsApi;
import com.spoonacular.client.ApiException;
import com.spoonacular.client.model.InlineResponse20024;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;

public class MainActivity extends AppCompatActivity {
    RecyclerView userIngredientRecyclerView;
    RecyclerView.LayoutManager layoutManager;
    UserIngredientDbAdapter userIngredientDbAdapter;
    Cursor userIngredientListCursor;
    UserIngredientListAdapter userIngredientListAdapter;
    TextInputLayout ingredientSearchInputLayout;
    FloatingActionButton findRecipesFAB;
    boolean hasListChanged = true;

    ArrayList<InlineResponse20024> searchResultData = new ArrayList<>();
    SharedPreferences sharedPreferences;

    public static String API_KEY = "f9dbf7d212034a9d92c0adc6161f7de2";
    public static String INGREDIENT_IMAGE_BASE_URL = "https://spoonacular.com/cdn/ingredients_250x250/";
    public static String RECIPE_IMAGE_BASE_URL = "https://spoonacular.com/recipeImages/";

    // Make search box lose focus on touch outside.
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    // Get API response when searching ingredients.
    private ArrayList<String> getAutoCompleteList(String query) {
        IngredientsApi apiInstance = new IngredientsApi();
        ArrayList<String> resultList = new ArrayList<>();

        try {
            List<InlineResponse20024> results = apiInstance.autocompleteIngredientSearch(API_KEY, query, 5, true, "");

            searchResultData.clear();
            for (InlineResponse20024 response : results) {
                resultList.add(response.getName());
                searchResultData.add(response);
            }
        } catch (ApiException e) {
            System.err.println("Exception when calling IngredientsApi#autocompleteIngredientSearch");
            e.printStackTrace();
        }

        return resultList;
    }

    // Add new user ingredient.
    private void addNewIngredient(AdapterView<?> adapterView, View view, int pos) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                IngredientDbAdapter ingredientDbAdapter = new IngredientDbAdapter(adapterView.getContext());
                ingredientDbAdapter.open();
                UserIngredientDbAdapter userIngredientDbAdapter = new UserIngredientDbAdapter(adapterView.getContext());
                userIngredientDbAdapter.open();

                int id = searchResultData.get(pos).getId();
                String name = searchResultData.get(pos).getName();
                String imageName = searchResultData.get(pos).getImage();

                // Update the database.
                Ingredient newIngredient = new Ingredient(id, name, imageName);
                try {
                    ingredientDbAdapter.insertIngredient(newIngredient);
                } catch (SQLiteConstraintException e) {
                    // Ingredient already in at least one of the database recipes.
                }

                try {
                    userIngredientDbAdapter.insertUserIngredient(new MeasuredIngredient(id, 0, "g"));
                } catch (SQLiteConstraintException e) {
                    // Ingredient already in the database.
                }

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(adapterView.getContext());
                sharedPreferences.edit().putBoolean("hasListChanged", true).apply();

                // Update the RecyclerView listing user ingredients.
                userIngredientDbAdapter.close();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        userIngredientListCursor.requery();
                        userIngredientListAdapter.notifyDataSetChanged();
                    }
                });

                // Get the ingredient image.
                byte[] decodedImage = getIngredientImage(imageName);
                ingredientDbAdapter.updateIngredient(id, newIngredient, decodedImage);

                // Update the RecyclerView with new image.
                ingredientDbAdapter.close();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        userIngredientListCursor.requery();
                        userIngredientListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    // Get ingredient image from the API.
    // Should be called asynchronously.
    private byte[] getIngredientImage(String image) {
        byte[] decodedImage = new byte[0];
        String imageUrl = INGREDIENT_IMAGE_BASE_URL + image;
        try {
            Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(imageUrl).getContent());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            decodedImage = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return decodedImage;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize shared preferences.
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Open the master database.
        DBAdapter dbAdapter = new DBAdapter(this);
        dbAdapter.open();

        // Set up ingredient list.
        userIngredientRecyclerView = findViewById(R.id.ingredientRecyclerView);
        layoutManager = new LinearLayoutManager(this);
        userIngredientRecyclerView.setLayoutManager(layoutManager);

        userIngredientDbAdapter = new UserIngredientDbAdapter(this);
        userIngredientDbAdapter.open();

        userIngredientListCursor = userIngredientDbAdapter.getUserIngredients();
        userIngredientListAdapter = new UserIngredientListAdapter(this, userIngredientListCursor);
        userIngredientRecyclerView.setAdapter(userIngredientListAdapter);

        // Set up ingredient search.
        ingredientSearchInputLayout = findViewById(R.id.ingredientSearchInputLayout);
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(this, R.layout.ingredient_auto_complete_holder, new String[0]);
        AutoCompleteTextView searchBar = (AutoCompleteTextView) ingredientSearchInputLayout.getEditText();
        assert searchBar != null;
        searchBar.setAdapter(autoCompleteAdapter);

        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.ingredientSearchInProgress);
                    progressBar.setVisibility(View.VISIBLE);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<String> searchResults = getAutoCompleteList(searchBar.getText().toString());
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        autoCompleteAdapter.clear();
                                        for (String result : searchResults)
                                            autoCompleteAdapter.add(result);
                                        autoCompleteAdapter.getFilter().filter(searchBar.getText(), null);
                                    } catch (UnsupportedOperationException e) {
                                        // Do nothing.
                                    }
                                    progressBar.setVisibility(View.INVISIBLE);
                                }
                            });
                        }
                    }).start();
                    return true;
                }
                return false;
            }
        });

        // Add a new ingredient.
        searchBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                searchBar.getText().clear();
                addNewIngredient(adapterView, view, i);
            }
        });

        // FAB.
        findRecipesFAB = findViewById(R.id.findRecipesFAB);
        findRecipesFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userIngredientListAdapter.getItemCount() == 0) {
                    Toast.makeText(getApplicationContext(), R.string.no_user_ingredients, Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent recipeSearch = new Intent(getApplicationContext(), RecipeListActivity.class);
                recipeSearch.putExtra("hasListChanged", sharedPreferences.getBoolean("hasListChanged", false));
                startActivity(recipeSearch);

                sharedPreferences.edit().putBoolean("hasListChanged", false).apply();
            }
        });

        dbAdapter.close();
    }
}