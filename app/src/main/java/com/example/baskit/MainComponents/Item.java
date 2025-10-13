package com.example.baskit.MainComponents;

public class Item
{
    protected String id = "";
    protected String name = "";
    protected double price = 0;
    protected int quantity = 1;
    protected boolean checked = false;
    protected Supermarket supermarket;

    public Item(String name)
    {
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

    public void setSupermarket(Supermarket supermarket) {
        this.supermarket = supermarket;
    }

    @Override
    public String toString() {
        return name;
    }
}
