package com.example.defridger;

import static com.example.defridger.MainActivity.API_KEY;
import static com.example.defridger.MainActivity.RECIPE_IMAGE_BASE_URL;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.example.defridger.adapters.IngredientDbAdapter;
import com.example.defridger.adapters.RecipeDbAdapter;
import com.example.defridger.adapters.RecipeDetailsDbAdapter;
import com.example.defridger.adapters.RecipeIngredientDbAdapter;
import com.example.defridger.adapters.RecipeListAdapter;
import com.example.defridger.adapters.UserIngredientDbAdapter;
import com.example.defridger.items.Ingredient;
import com.example.defridger.items.MeasuredIngredient;
import com.example.defridger.items.Recipe;
import com.example.defridger.items.RecipeDetails;
import com.spoonacular.RecipesApi;
import com.spoonacular.client.ApiException;
import com.spoonacular.client.model.InlineResponse2001;
import com.spoonacular.client.model.InlineResponse20024;
import com.spoonacular.client.model.InlineResponse2003ExtendedIngredients;
import com.spoonacular.client.model.InlineResponse2004;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RecipeListActivity extends AppCompatActivity {
    RecyclerView recipeRecyclerView;
    RecyclerView.LayoutManager layoutManager;
    RecipeDetailsDbAdapter recipeDetailsDbAdapter;
    Cursor recipeListCursor;
    RecipeListAdapter recipeListAdapter;
    ProgressBar recipeListProgressBar;

    // Get recipes matching user ingredients from the API.
    // Should be called asynchronously.
    private List<InlineResponse2001> getNewRecipes(Context context) {
        RecipesApi apiInstance = new RecipesApi();
        ArrayList<String> resultList = new ArrayList<>();

        UserIngredientDbAdapter userIngredientDbAdapter = new UserIngredientDbAdapter(context);
        userIngredientDbAdapter.open();

        ArrayList<String> userIngredients = userIngredientDbAdapter.getUserIngredientsList();
        String userIngredientsString = String.join(",", userIngredients);

        List<InlineResponse2001> result = null;
        try {
            result = apiInstance.searchRecipesByIngredients(API_KEY, userIngredientsString, 4, false, BigDecimal.valueOf(1), true);
        } catch (ApiException e) {
            System.err.println("Exception when calling RecipesApi#searchRecipesByIngredients");
            e.printStackTrace();
        }

        userIngredientDbAdapter.close();
        return result;
    }

    private List<InlineResponse2004> getRecipesDetails(Context context, ArrayList<String> ids) {
        RecipesApi apiInstance = new RecipesApi();
        String idsString = String.join(",", ids);

        List<InlineResponse2004> result = null;
        try {
            result = apiInstance.getRecipeInformationBulk(API_KEY, idsString, false);
        } catch (ApiException e) {
            System.err.println("Exception when calling RecipesApi#getRecipeInformationBulk");
            e.printStackTrace();
        }

        return result;
    }

    // Insert new recipes into the database.
    private void addNewRecipes(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<InlineResponse2001> recipesData = getNewRecipes(context);
                if (recipesData.isEmpty()) {
                    // Update the RecyclerView listing recipes.
                    RecipeListActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recipeListCursor.requery();
                            recipeListAdapter.notifyDataSetChanged();
                            recipeListProgressBar.setVisibility(View.GONE);
                            recipeRecyclerView.setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }

                IngredientDbAdapter ingredientDbAdapter = new IngredientDbAdapter(context);
                ingredientDbAdapter.open();
                RecipeIngredientDbAdapter recipeIngredientDbAdapter = new RecipeIngredientDbAdapter(context);
                recipeIngredientDbAdapter.open();
                RecipeDbAdapter recipeDbAdapter = new RecipeDbAdapter(context);
                recipeDbAdapter.open();
                RecipeDetailsDbAdapter recipeDetailsDbAdapter = new RecipeDetailsDbAdapter(context);
                recipeDetailsDbAdapter.open();

                ArrayList<String> ids = new ArrayList<>();
                for (InlineResponse2001 recipeData : recipesData)
                    ids.add(String.valueOf(recipeData.getId()));

                List<InlineResponse2004> recipesDetails = getRecipesDetails(context, ids);

                for (InlineResponse2004 recipeDetails : recipesDetails) {
                    int id = recipeDetails.getId();
                    String name = recipeDetails.getTitle();
                    String imageType = recipeDetails.getImageType();

                    Recipe newRecipe = new Recipe(id, name, imageType);
                    try {
                        recipeDbAdapter.insertRecipe(newRecipe);
                    } catch (SQLiteConstraintException e) {
                        // Recipe already in the database.
                        continue;
                    }

                    List<InlineResponse2003ExtendedIngredients> recipeIngredients = recipeDetails.getExtendedIngredients();
                    for (InlineResponse2003ExtendedIngredients recipeIngredient : recipeIngredients) {
                        int ingredientID = recipeIngredient.getId();
                        String ingredientName = recipeIngredient.getName();
                        String ingredientImageName = recipeIngredient.getImage();
                        float ingredientAmount = recipeIngredient.getAmount().floatValue();
                        String ingredientUnit = recipeIngredient.getUnit();

                        Ingredient ingredient = new Ingredient(ingredientID, ingredientName, ingredientImageName);
                        try {
                            ingredientDbAdapter.insertIngredient(ingredient);
                        } catch (SQLiteConstraintException e) {
                            // Ingredient already in the database.
                        }

                        MeasuredIngredient measuredIngredient = new MeasuredIngredient(ingredientID, ingredientAmount, ingredientUnit);
                        try {
                            recipeIngredientDbAdapter.insertRecipeIngredient(newRecipe, measuredIngredient);
                        } catch (SQLiteConstraintException e) {
                            // Ingredient already in the database.
                        }
                    }

                    String author = recipeDetails.getCreditsText();
                    int servings = recipeDetails.getServings().intValue();
                    int time = recipeDetails.getReadyInMinutes();
                    String sourceURL = recipeDetails.getSourceUrl();
                    boolean isVegan = recipeDetails.getVegan();
                    boolean isVegetarian = recipeDetails.getVegetarian();

                    RecipeDetails newRecipeDetails = new RecipeDetails(id, id, author, servings, time, sourceURL, isVegan, isVegetarian);
                    try {
                        recipeDetailsDbAdapter.insertRecipeDetails(newRecipeDetails);
                    } catch (SQLiteConstraintException e) {
                        // Details already in the database.
                    }
                }

                // Update the RecyclerView listing recipes.
                RecipeListActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recipeListCursor.requery();
                        recipeListAdapter.notifyDataSetChanged();
                        recipeListProgressBar.setVisibility(View.GONE);
                        recipeRecyclerView.setVisibility(View.VISIBLE);
                    }
                });

                // Get the recipes' images.
                for (InlineResponse2001 recipeData : recipesData) {
                    int id = recipeData.getId();
                    String name = recipeData.getTitle();
                    String imageType = recipeData.getImageType();

                    Recipe recipe = new Recipe(id, name, imageType);
                    byte[] decodedImage = getRecipeImage(id, imageType);
                    recipeDbAdapter.updateRecipe(id, recipe, decodedImage);
                }

                // Update the RecyclerView with new image.
                RecipeListActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recipeListCursor.requery();
                        recipeListAdapter.notifyDataSetChanged();
                    }
                });

                recipeDetailsDbAdapter.close();
                ingredientDbAdapter.close();
                recipeIngredientDbAdapter.close();
                recipeDbAdapter.close();
            }
        }).start();
    }

    // Get recipe image from the API.
    // Should be called asynchronously.
    private byte[] getRecipeImage(int id, String imageType) {
        byte[] decodedImage = new byte[0];
        String imageUrl = RECIPE_IMAGE_BASE_URL + String.valueOf(id) + "-240x150." + imageType;
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
        setContentView(R.layout.activity_recipe_list);

        ImageButton backButton = findViewById(R.id.recipeListBackButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecipeListActivity.this.finish();
            }
        });

        // Set up recipe list.
        recipeRecyclerView = findViewById(R.id.recipeRecyclerView);
        layoutManager = new LinearLayoutManager(this);
        recipeRecyclerView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recipeRecyclerView.getContext(),
                ((LinearLayoutManager) layoutManager).getOrientation());
        recipeRecyclerView.addItemDecoration(dividerItemDecoration);

        recipeDetailsDbAdapter = new RecipeDetailsDbAdapter(this);
        recipeDetailsDbAdapter.open();

        recipeListCursor = recipeDetailsDbAdapter.getAllRecipesDetails();
        recipeListAdapter = new RecipeListAdapter(this, recipeListCursor);
        recipeRecyclerView.setAdapter(recipeListAdapter);

        // Get the recipes.
        recipeListProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (getIntent().getBooleanExtra("hasListChanged", true)) {
            recipeRecyclerView.setVisibility(View.INVISIBLE);
            recipeListProgressBar.setVisibility(View.VISIBLE);
            addNewRecipes(this);
        }
    }
}