package com.example.baskit.MainComponents;

import java.util.Objects;

public class Supermarket implements Cloneable
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
    public String toString()
    {
        if (section.isEmpty())
        {
            return supermarket;
        }

        return supermarket + " ," + section;
    }

    @Override
    public Supermarket clone()
    {
        try
        {
            return (Supermarket) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Supermarket other = (Supermarket) obj;
        return supermarket.equals(other.supermarket) &&
                section.equals(other.section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supermarket, section);
    }
}
