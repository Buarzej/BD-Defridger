package com.example.defridger.items;

public class RecipeDetails {
    public int id;
    public int recipeID;
    public String author;
    public int servings;
    public int time;
    public String sourceURL;
    public boolean isVegan;
    public boolean isVegetarian;

    public RecipeDetails() {
    }

    public RecipeDetails(int id, int recipeID, String author, int servings, int time, String sourceURL, boolean isVegan, boolean isVegetarian) {
        this.id = id;
        this.recipeID = recipeID;
        this.author = author;
        this.servings = servings;
        this.time = time;
        this.sourceURL = sourceURL;
        this.isVegan = isVegan;
        this.isVegetarian = isVegetarian;
    }
}
