package com.example.baskit.MainComponents;

public class Supermarket
{
    private String supermarket;
    private String section;

    public Supermarket() {}

    public Supermarket(String supermarket)
    {
        this.supermarket = supermarket;
    }

    public Supermarket(String supermarket, String section) {
        this.supermarket = supermarket;
        this.section = section;
    }

    public String getSupermarket() {
        return supermarket;
    }

    public void setSupermarket(String supermarket) {
        this.supermarket = supermarket;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    @Override
    public String toString() {
        return section + " ," + supermarket;
    }
}
