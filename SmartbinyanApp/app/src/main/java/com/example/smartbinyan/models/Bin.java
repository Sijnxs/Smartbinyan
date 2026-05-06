package com.example.smartbinyan.models;

public class Bin {

    private int id;
    private String name;
    private int fillLevel;   // % full
    private int gasPercent;  // % gas level
    private Toxicity toxicity;

    // Enum for toxicity
    public enum Toxicity {
        SAFE,
        WARNING,
        HAZARD
    }

    // Default constructor (required for Firebase if needed)
    public Bin() {}

    // Full constructor
    public Bin(int id, String name, int fillLevel, int gasPercent, Toxicity toxicity) {
        this.id = id;
        this.name = name;
        this.fillLevel = fillLevel;
        this.gasPercent = gasPercent;
        this.toxicity = toxicity;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getFillLevel() { return fillLevel; }
    public int getGasPercent() { return gasPercent; }
    public Toxicity getToxicity() { return toxicity; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setFillLevel(int fillLevel) { this.fillLevel = fillLevel; }
    public void setGasPercent(int gasPercent) { this.gasPercent = gasPercent; }
    public void setToxicity(Toxicity toxicity) { this.toxicity = toxicity; }
}
