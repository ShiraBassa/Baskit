package com.example.baskit.MainComponents;

import androidx.annotation.Nullable;

public class PriceRow
{
    private Supermarket supermarket;
    private double price;
    private ItemInfo info;

    public PriceRow(Supermarket supermarket, double price, ItemInfo info)
    {
        this.supermarket = supermarket;
        this.price = price;
        this.info = info;
    }

    public Supermarket getSupermarket() {
        return supermarket;
    }

    public void setSupermarket(Supermarket supermarket) {
        this.supermarket = supermarket;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public ItemInfo getInfo() {
        return info;
    }

    public void setInfo(ItemInfo info) {
        this.info = info;
    }

    @Override
    public boolean equals(@Nullable Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof PriceRow)) return false;

        PriceRow other = (PriceRow) obj;

        return Double.compare(other.price, price) == 0 &&
                supermarket.equals(other.supermarket) &&
                info.equals(other.info);
    }

    @Override
    public int hashCode()
    {
        return java.util.Objects.hash(supermarket, price, info);
    }
}
