package com.example.baskit.MainComponents;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class Item implements Cloneable
{
    private final String ID_PREFIX = "item_";

    protected String id = "";
    protected String name = "";
    protected double price = 0;
    protected int quantity = 1;
    protected boolean checked = false;
    protected Supermarket supermarket;

    public Item(){}

    public Item(String name)
    {
        this.name = name;
    }

    public Item(String id, String name) {
        this.id = ID_PREFIX + id;
        this.name = name;
    }

    public Item(Item item)
    {
        this.id = item.getId();
        this.name = item.getName();
        this.price = item.getPrice();
        this.quantity = item.getQuantity();
        this.checked = item.isChecked();
        this.supermarket = item.getSupermarket();
    }

    public Item(String name, int quantity)
    {
        this.name = name;
        this.quantity = quantity;
    }

    public Item(String name, int quantity, int price)
    {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
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
    public String toString() {
        return name;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", getId());
        result.put("name", getName());
        result.put("quantity", getQuantity());
        result.put("checked", isChecked());
        // Add any other fields your Item has
        return result;
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
}
