package com.example.baskit.main_components;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.baskit.Baskit;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("unused")
@IgnoreExtraProperties
public class Item implements Cloneable
{
    protected String id = "";
    protected String baseName = "";
    protected double price = 0;
    protected int quantity = 1;
    protected boolean checked = false;
    protected Supermarket supermarket;
    protected String company;
    protected Double weight;
    protected String unit;

    @Exclude
    private static final String ID_PREFIX = "item_";

    public Item() {}

    public Item(String baseName)
    {
        setBaseName(baseName);
    }

    public Item(String id, String baseName)
    {
        setId(id);
        setBaseName(baseName);
    }

    public Item(Item item)
    {
        setId(item.getId());
        setBaseName(item.getBaseName());
        this.price = item.getPrice();
        this.quantity = item.getQuantity();
        this.checked = item.isChecked();
        this.supermarket = item.getSupermarket();
        this.company = item.getCompany();
        this.weight = item.getWeight();
        this.unit = item.getUnit();
    }

    public Item(String baseName, int quantity)
    {
        setBaseName(baseName);
        this.quantity = quantity;
    }

    public Item(String baseName, int quantity, int price)
    {
        setBaseName(baseName);
        this.quantity = quantity;
        this.price = price;
    }

    public String getCompany()
    {
        return company;
    }

    @SuppressWarnings("unused")
    public void setCompany(String company)
    {
        this.company = company;
    }

    public Double getWeight()
    {
        return weight;
    }

    @SuppressWarnings("unused")
    public void setWeight(Double weight)
    {
        this.weight = weight;
    }

    public String getUnit()
    {
        return unit;
    }

    @SuppressWarnings("unused")
    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public String getBaseName()
    {
        return baseName;
    }

    public void setBaseName(String baseName)
    {
        this.baseName = Baskit.encodeKey(baseName);
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

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        if (id.isEmpty()) return;
        this.id = isFullId(id) ? id : getFullId(id);
    }

    public Supermarket getSupermarket()
    {
        return supermarket;
    }

    @SuppressWarnings("unused")
    public void setSupermarket(Supermarket supermarket)
    {
        this.supermarket = supermarket;
    }

    @Exclude
    public String getDecodedName()
    {
        return Baskit.decodeKey(this.baseName);
    }

    @Exclude
    public double getTotal()
    {
        return Math.round(this.price * this.quantity * 100.0) / 100.0;
    }

    @Exclude
    public static boolean isFullId(String id)
    {
        return id.startsWith(ID_PREFIX);
    }

    @Exclude
    public String getAbsoluteId()
    {
        return isFullId(id) ? id.substring(ID_PREFIX.length()) : id;
    }

    @Exclude
    public static String getFullId(String id)
    {
        return isFullId(id) ? id : ID_PREFIX + id;
    }

    @Exclude
    public int raiseQuantity()
    {
        return ++quantity;
    }

    @Exclude
    public int lowerQuantity()
    {
        return --quantity;
    }

    @NonNull
    @Override
    public String toString()
    {
        return getBaseName();
    }

    @Exclude
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

    @Exclude
    public boolean isPriceKnown()
    {
        if (supermarket == null)
        {
            return false;
        }

        return !supermarket.equals(Baskit.UNASSIGNED_SUPERMARKET) &&
                price != 0;
    }

    @Exclude
    public boolean isUnassignedToSupermarket()
    {
        if (supermarket == null)
        {
            return true;
        }

        return supermarket.equals(Baskit.UNASSIGNED_SUPERMARKET);
    }

    @Exclude
    public void setUnchosen()
    {
        this.price = 0.0;
        this.supermarket = null;
        this.company = null;
        this.weight = null;
        this.unit = null;
        this.id = "";
    }

    @Exclude
    public void fillInfo(ItemInfo info)
    {
        this.company = info.getCompany();
        this.weight = info.getWeight();
        this.unit = info.getUnit();
        setId(info.getCode());
    }

    @Exclude
    public void fillVariant(ItemVariant variant)
    {
        fillInfo(variant.getInfo());
        this.price = variant.getPrice();
        this.supermarket = variant.getSupermarket();
    }

    @Exclude
    public ItemVariant getCheapestVariant(ArrayList<ItemVariant> variants)
    {
        if (variants == null || variants.isEmpty())
        {
            return null;
        }

        ItemVariant ogVariant = getVariant();
        double cheapestPrice = Double.MAX_VALUE;
        ItemVariant cheapestVariant = null;

        for (ItemVariant variant : variants)
        {
            if (!isVariantOf(variant)) continue;

            ItemInfo info = variant.getInfo();

            double currPrice = variant.getPrice();
            if (currPrice == 0.0) continue;

            if (cheapestVariant == null)
            {
                cheapestVariant = variant;
                cheapestPrice = currPrice;
                continue;
            }

            boolean hasCurrentCompany = this.company != null && !this.company.isEmpty();

            int currScoreCompany = (hasCurrentCompany && Objects.equals(this.company, info.getCompany())) ? 1 : 0;
            int bestScoreCompany = (hasCurrentCompany && Objects.equals(this.company, cheapestVariant.getInfo().getCompany())) ? 1 : 0;

            boolean hasCurrentSupermarket = this.supermarket != null && !this.supermarket.equals(Baskit.UNASSIGNED_SUPERMARKET);

            int currScoreSupermarket = (hasCurrentSupermarket && variant.getSupermarket() != null &&
                    variant.getSupermarket().equals(this.supermarket)) ? 1 : 0;

            int bestScoreSupermarket = (hasCurrentSupermarket && cheapestVariant.getSupermarket() != null &&
                    cheapestVariant.getSupermarket().equals(this.supermarket)) ? 1 : 0;

            boolean better = false;

            if (currPrice < cheapestPrice)
            {
                better = true;
            }
            else if (Double.compare(currPrice, cheapestPrice) == 0)
            {
                if (currScoreCompany > bestScoreCompany)
                {
                    better = true;
                }
                else if (currScoreCompany == bestScoreCompany && currScoreSupermarket > bestScoreSupermarket)
                {
                    better = true;
                }
            }

            if (better)
            {
                cheapestVariant = variant;
                cheapestPrice = currPrice;
            }
        }

        if (cheapestVariant != null && cheapestVariant.getPrice() == price)
        {
            return ogVariant;
        }

        return cheapestVariant;
    }

    @Exclude
    public void setCheapestVariant(ArrayList<ItemVariant> variants)
    {
        ItemVariant variant = getCheapestVariant(variants);

        if (variant != null)
        {
            fillVariant(variant);
        }
    }

    @Exclude
    public ItemVariant getSupermarketVariant(
            Supermarket supermarket,
            ArrayList<ItemVariant> variants)
    {
        if (variants == null || variants.isEmpty())
        {
            return null;
        }

        ItemVariant ogVariant = getVariant();
        ItemVariant cheapestVariant = null;
        double cheapestPrice = Double.MAX_VALUE;

        for (ItemVariant variant : variants)
        {
            if (!isVariantOf(variant, supermarket)) continue;

            ItemInfo info = variant.getInfo();
            double currPrice = variant.getPrice();

            if (cheapestVariant == null)
            {
                cheapestVariant = variant;
                cheapestPrice = currPrice;
                continue;
            }

            boolean hasCurrentCompany = this.company != null && !this.company.isEmpty();

            int currScoreCompany = (hasCurrentCompany && Objects.equals(this.company, info.getCompany())) ? 1 : 0;
            ItemInfo bestInfo = cheapestVariant.getInfo();
            int bestScoreCompany =
                    (hasCurrentCompany && Objects.equals(this.company, bestInfo.getCompany())) ? 1 : 0;
            boolean better = false;

            if (currPrice < cheapestPrice)
            {
                better = true;
            }
            else if (Double.compare(currPrice, cheapestPrice) == 0)
            {
                if (currScoreCompany > bestScoreCompany)
                {
                    better = true;
                }
            }

            if (better)
            {
                cheapestVariant = variant;
                cheapestPrice = currPrice;
            }
        }

        if (this.supermarket == supermarket && Objects.requireNonNull(cheapestVariant).getPrice() == price)
        {
            return ogVariant;
        }

        return cheapestVariant;
    }

    @Exclude
    public void setSupermarketVariant(
            Supermarket supermarket,
            ArrayList<ItemVariant> variants)
    {
        ItemVariant variant = getSupermarketVariant(supermarket, variants);

        if (variant != null)
        {
            fillVariant(variant);
        }
        else
        {
            setUnchosen();
        }
    }

    @Exclude
    public ItemInfo getInfo()
    {
        Double safeWeight = (weight != null) ? weight : 0.0;
        String safeCompany = (company != null) ? company : "";
        String safeUnit = (unit != null) ? unit : "";

        return new ItemInfo(getAbsoluteId(), baseName, safeCompany, safeWeight, safeUnit);
    }

    @Exclude
    public ItemVariant getVariant()
    {
        return new ItemVariant(supermarket, price, getInfo());
    }

    @Exclude
    public boolean isVariantOf(ItemVariant variant)
    {
        if (variant == null || variant.getInfo() == null)
        {
            return false;
        }

        if (supermarket == null)
        {
            return true;
        }

        if (variant.getSupermarket() == null)
        {
            return false;
        }

        ItemInfo info = variant.getInfo();
        Double vWeight = info.getWeight();
        String vUnit = info.getUnit();

        boolean sameWeight =
                (weight == null && vWeight == null) ||
                        (weight != null && vWeight != null && Double.compare(weight, vWeight) == 0);

        boolean sameUnit =
                (unit == null && vUnit == null) ||
                        (unit != null && unit.equals(vUnit));

        return sameWeight && sameUnit;
    }

    @Exclude
    public boolean isVariantOf(ItemVariant variant, Supermarket newSupermarket)
    {
        if (variant == null || variant.getInfo() == null)
        {
            return false;
        }

        if (variant.getSupermarket() == null || !variant.getSupermarket().equals(newSupermarket))
        {
            return false;
        }

        if (supermarket == null)
        {
            return true;
        }

        ItemInfo info = variant.getInfo();
        Double vWeight = info.getWeight();
        String vUnit = info.getUnit();

        boolean sameWeight =
                (weight == null && vWeight == null) ||
                        (weight != null && vWeight != null && Double.compare(weight, vWeight) == 0);

        boolean sameUnit =
                (unit == null && vUnit == null) ||
                        (unit != null && unit.equals(vUnit));

        return sameWeight && sameUnit;
    }

    @Exclude
    public boolean isIdenticalVariantOf(ItemVariant variant)
    {
        return variant.getInfo().equals(getInfo()) &&
                variant.getSupermarket().equals(supermarket);
    }


    @SuppressWarnings("unused")
    public static class ItemInfo
    {
        private String code;
        private String baseName;
        private String company;
        private Double weight;
        private String unit;

        public ItemInfo(String code, String baseName,
                        String company, Double weight, String unit)
        {
            this.code = code;
            this.baseName = baseName;
            this.company = company;
            this.weight = weight;
            this.unit = unit;
        }

        public String getCode() {
            return code;
        }

        @SuppressWarnings("unused")
        public void setCode(String code) {
            this.code = code;
        }

        public String getBaseName() {
            return baseName;
        }

        @SuppressWarnings("unused")
        public void setBaseName(String baseName) {
            this.baseName = baseName;
        }

        public String getCompany() {
            return company;
        }

        @SuppressWarnings("unused")
        public void setCompany(String company) {
            this.company = company;
        }

        public Double getWeight()
        {
            return weight;
        }

        public String getWeightStr()
        {
            if (weight == null || weight == 0.0) return "";

            if (weight == weight.longValue())
            {
                return String.valueOf(weight.longValue());
            }
            else
            {
                java.math.BigDecimal bd = java.math.BigDecimal.valueOf(weight);
                bd = bd.stripTrailingZeros();
                return bd.toPlainString();
            }
        }

        @SuppressWarnings("unused")
        public void setWeight(Double weight) {
            this.weight = weight;
        }

        public String getUnit() {
            return unit;
        }

        @SuppressWarnings("unused")
        public void setUnit(String unit)
        {
            this.unit = unit;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            ItemInfo other = (ItemInfo) obj;

            return baseName.equals(other.baseName) &&
                    company.equals(other.company) &&
                    weight.equals(other.weight) &&
                    unit.equals(other.unit);
        }

        @Override
        public int hashCode()
        {
            return java.util.Objects.hash(baseName, company, weight, unit);
        }

        public String getFullMeasureStr()
        {
            String str = "";

            if (weight != null && weight != 0.0)
            {
                str += getWeightStr();

                if (unit != null && !unit.isEmpty()  && !unit.equals("לא ידוע"))
                {
                    str += " " + unit;
                }
            }

            return str;
        }
    }


    @SuppressWarnings("unused")
    public static class ItemVariant
    {
        private Supermarket supermarket;
        private double price;
        private ItemInfo info;

        public ItemVariant(Supermarket supermarket, double price, ItemInfo info)
        {
            this.supermarket = supermarket;
            this.price = price;
            this.info = info;
        }

        public Supermarket getSupermarket() {
            return supermarket;
        }

        @SuppressWarnings("unused")
        public void setSupermarket(Supermarket supermarket) {
            this.supermarket = supermarket;
        }

        public double getPrice() {
            return price;
        }

        @SuppressWarnings("unused")
        public void setPrice(double price) {
            this.price = price;
        }

        public ItemInfo getInfo() {
            return info;
        }

        @SuppressWarnings("unused")
        public void setInfo(ItemInfo info) {
            this.info = info;
        }

        @Override
        public boolean equals(@Nullable Object obj)
        {
            if (this == obj) return true;
            if (!(obj instanceof ItemVariant)) return false;

            ItemVariant other = (ItemVariant) obj;

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
}