package com.example.baskit.MainComponents;

import androidx.annotation.NonNull;

import com.example.baskit.Baskit;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Item implements Cloneable
{
    private static final String ID_PREFIX = "item_";

    protected String id = "";
    protected String name = "";
    protected double price = 0;
    protected int quantity = 1;
    protected boolean checked = false;
    protected Supermarket supermarket;

    public Item(){}

    public Item(String name)
    {
        setName(name);
    }

    public Item(String id, String name) {
        this.id = ID_PREFIX + id;
        setName(name);
    }

    public Item(Item item)
    {
        this.id = item.getId();
        setName(item.getName());
        this.price = item.getPrice();
        this.quantity = item.getQuantity();
        this.checked = item.isChecked();
        this.supermarket = item.getSupermarket();
    }

    public Item(String name, int quantity)
    {
        setName(name);
        this.quantity = quantity;
    }

    public Item(String name, int quantity, int price)
    {
        setName(name);
        this.quantity = quantity;
        this.price = price;
    }

    public String getName()
    {
        return name;
    }

    public String getDecodedName()
    {
        return decodeKey(this.name);
    }

    public void setName(String name)
    {
        this.name = encodeKey(name);
    }

    public double getPrice() {
        return price;
    }

    @Exclude
    public double getTotal()
    {
        return Math.round(this.price * this.quantity * 100.0) / 100.0;
    }

    @Exclude
    public double getTotal(double otherPrice)
    {
        return Math.round(otherPrice * this.quantity * 100.0) / 100.0;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getId() {
        return id;
    }

    @Exclude
    public String getAbsoluteId()
    {
        return id.startsWith(ID_PREFIX) ? id.substring(ID_PREFIX.length()) : id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void updateId(String id)
    {
        this.id = ID_PREFIX + id;
    }

    public int raiseQuantity()
    {
        return ++quantity;
    }

    public int lowerQuantity()
    {
        return --quantity;
    }

    public boolean hasSupermarket()
    {
        return this.supermarket != null;
    }

    public Supermarket getSupermarket() {
        return supermarket;
    }

    public void setSupermarket(Supermarket supermarket)
    {
        this.supermarket = supermarket;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @NonNull
    @Override
    public Item clone()
    {
        try
        {
            Item clone = (Item) super.clone();
            clone.supermarket = this.supermarket != null ? this.supermarket.clone() : null;
            return clone;
        }
        catch (CloneNotSupportedException e)
        {
            throw new AssertionError();
        }
    }

    public boolean isPriceKnown()
    {
        if (supermarket == null)
        {
            return false;
        }

        if (supermarket.equals(Baskit.UNASSIGNED_SUPERMARKET) ||
                price == 0)
        {
            return false;
        }

        return true;
    }

    public boolean isUnassigned()
    {
        if (supermarket == null)
        {
            return true;
        }

        if (supermarket.equals(Baskit.UNASSIGNED_SUPERMARKET))
        {
            return true;
        }

        return false;
    }

   public static String encodeKey(String s)
    {
        if (s == null) return null;
        String out = s;
        out = out.replace(".", "__dot__");
        out = out.replace("$", "__dollar__");
        out = out.replace("#", "__hash__");
        out = out.replace("[", "__lbracket__");
        out = out.replace("]", "__rbracket__");
        out = out.replace("/", "__slash__");
        return out;
    }

    public static String decodeKey(String s)
    {
        if (s == null) return null;
        String out = s;
        out = out.replace("__dot__", ".");
        out = out.replace("__dollar__", "$");
        out = out.replace("__hash__", "#");
        out = out.replace("__lbracket__", "[");
        out = out.replace("__rbracket__", "]");
        out = out.replace("__slash__", "/");
        return out;
    }
}