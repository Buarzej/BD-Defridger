package com.example.defridger.items;

public class MeasuredIngredient {
    public int ingredientID;
    public float amount;
    public String unit;

    public MeasuredIngredient() {
    }

    public MeasuredIngredient(int ingredientID, float amount, String unit) {
        this.ingredientID = ingredientID;
        this.amount = amount;
        this.unit = unit;
    }
}
