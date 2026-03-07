package com.example.baskit.MainComponents;

import com.example.baskit.Categories.ItemViewPricesAdapter;

import androidx.annotation.NonNull;

import com.example.baskit.Baskit;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@IgnoreExtraProperties
public class Item implements Cloneable
{
    @Exclude
    private static final String ID_PREFIX = "item_";

    protected String id = "";
    protected String baseName = "";
    protected double price = 0;
    protected int quantity = 1;
    protected boolean checked = false;
    protected Supermarket supermarket;
    protected String company;
    protected Double weight;
    protected String unit;

    public Item(){}

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

    public void setCompany(String company)
    {
        this.company = company;
    }

    public Double getWeight()
    {
        return weight;
    }

    public void setWeight(Double weight)
    {
        this.weight = weight;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public String getBaseName()
    {
        return baseName;
    }

    @Exclude
    public String getDecodedName()
    {
        return Baskit.decodeKey(this.baseName);
    }

    public void setBaseName(String baseName)
    {
        this.baseName = Baskit.encodeKey(baseName);
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

    @Exclude
    public static double getTotal(double otherPrice, int otherQuantity)
    {
        return Math.round(otherPrice * otherQuantity * 100.0) / 100.0;
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

    @Exclude
    public String getAbsoluteId()
    {
        return getAbsoluteId(id);
    }

    @Exclude
    public static String getAbsoluteId(String id)
    {
        return isFullId(id) ? id.substring(ID_PREFIX.length()) : id;
    }

    public void setId(String id)
    {
        if (id.isEmpty()) return;
        this.id = isFullId(id) ? id : getFullId(id);
    }

    @Exclude
    public String getFullId()
    {
        return id.isEmpty() ? "" : getFullId(id);
    }

    @Exclude
    public static String getFullId(String id)
    {
        return isFullId(id) ? id : ID_PREFIX + id;
    }

    @Exclude
    public static boolean isFullId(String id)
    {
        return id.startsWith(ID_PREFIX);
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

    public void setSupermarket(Supermarket supermarket, Double price)
    {
        this.supermarket = supermarket;
        this.price = price;
    }

    @Exclude
    public void setSupermarketFromSmMap(Supermarket supermarket, Map<Supermarket, Double> prices)
    {
        if (prices == null || !prices.containsKey(supermarket))
        {
            return;
        }

        this.supermarket = supermarket;
        this.price = prices.get(supermarket);
    }

    @Exclude
    public void setSupermarketFromStringsMap(Supermarket supermarket, Map<String, Map<String, Double>> prices)
    {
        Map<Supermarket, Double> pricesSm = Supermarket.getSupermarketsPricesFromStrings(prices);
        setSupermarketFromSmMap(supermarket, pricesSm);
    }

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

        if (supermarket.equals(Baskit.UNASSIGNED_SUPERMARKET) ||
                price == 0)
        {
            return false;
        }

        return true;
    }

    @Exclude
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

    @Exclude
    public ItemViewPricesAdapter.PriceRow getCheapestRow(ArrayList<ItemViewPricesAdapter.PriceRow> priceRows)
    {
        if (priceRows == null || priceRows.isEmpty())
        {
            return null;
        }

        ItemViewPricesAdapter.PriceRow ogRow = null;
        double cheapestPrice = Double.MAX_VALUE;
        ItemViewPricesAdapter.PriceRow cheapestRow = null;

        for (ItemViewPricesAdapter.PriceRow row : priceRows)
        {
            if (row == null) continue;

            ItemInfo info = row.getInfo();
            if (info == null) continue;

            if (info.getCode().equals(this.getAbsoluteId()))
            {
                ogRow = row;
            }

            Double currWeight = info.getWeight();
            String currUnit = info.getUnit();
            double currPrice = row.getPrice();
            if (currPrice == 0.0) continue;

            // determine if this item actually defines a variant
            boolean hasWeightUnit = this.weight != null && this.unit != null && !this.unit.isEmpty();

            // compare variant safely
            boolean sameWeight =
                    (this.weight == null && currWeight == null) ||
                    (this.weight != null && currWeight != null && Double.compare(this.weight, currWeight) == 0);

            boolean sameUnit =
                    (this.unit == null && currUnit == null) ||
                    (this.unit != null && this.unit.equals(currUnit));

            boolean sameWeightAndUnit = sameWeight && sameUnit;

            // If the item defines a variant (weight+unit), restrict only by the variant itself.
            // Do NOT restrict by supermarket — we still want to allow switching supermarkets.
            if (hasWeightUnit && !sameWeightAndUnit)
            {
                continue;
            }

            if (cheapestRow == null)
            {
                cheapestRow = row;
                cheapestPrice = currPrice;
                continue;
            }

            boolean hasCurrentCompany = this.company != null && !this.company.isEmpty();

            int currScoreCompany = (hasCurrentCompany && Objects.equals(this.company, info.getCompany())) ? 1 : 0;
            int bestScoreCompany = (hasCurrentCompany && Objects.equals(this.company, cheapestRow.getInfo().getCompany())) ? 1 : 0;

            boolean hasCurrentSupermarket = this.supermarket != null && !this.supermarket.equals(Baskit.UNASSIGNED_SUPERMARKET);

            int currScoreSupermarket = (hasCurrentSupermarket && row.getSupermarket() != null &&
                    row.getSupermarket().equals(this.supermarket)) ? 1 : 0;

            int bestScoreSupermarket = (hasCurrentSupermarket && cheapestRow.getSupermarket() != null &&
                    cheapestRow.getSupermarket().equals(this.supermarket)) ? 1 : 0;

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
                cheapestRow = row;
                cheapestPrice = currPrice;
            }
        }

        if (cheapestPrice < price || ogRow == null)
        {
            return cheapestRow;
        }

        return ogRow;
    }

    @Exclude
    public void setCheapestRow(ArrayList<ItemViewPricesAdapter.PriceRow> priceRows)
    {
        ItemViewPricesAdapter.PriceRow row = getCheapestRow(priceRows);

        if (row != null)
        {
            fillRow(row);
        }
    }

    @Exclude
    public ItemViewPricesAdapter.PriceRow getSupermarketRow(
            Supermarket supermarket,
            ArrayList<ItemViewPricesAdapter.PriceRow> rows)
    {
        if (rows == null || rows.isEmpty())
        {
            return null;
        }

        ItemViewPricesAdapter.PriceRow ogRow = null;
        Double weight = this.getWeight();
        String unit = this.getUnit();

        ItemViewPricesAdapter.PriceRow cheapestRow = null;
        double cheapestPrice = Double.MAX_VALUE;

        for (ItemViewPricesAdapter.PriceRow row : rows)
        {
            if (row == null || row.getInfo() == null)
            {
                continue;
            }

            // If this item already has a supermarket, rows without supermarket are invalid.
            // But if the item itself has no supermarket, we allow rows without supermarket.
            if (row.getSupermarket() == null && this.supermarket != null)
            {
                continue;
            }

            if (supermarket != null && row.getSupermarket() != null && !row.getSupermarket().equals(supermarket))
            {
                continue;
            }

            ItemInfo info = row.getInfo();

            if (info.getCode().equals(this.getAbsoluteId()))
            {
                ogRow = row;
            }

            Double vWeight = info.getWeight();
            String vUnit = info.getUnit();

            boolean sameWeight =
                    (weight == null && vWeight == null) ||
                            (weight != null && vWeight != null && Double.compare(weight, vWeight) == 0);

            boolean sameUnit =
                    (unit == null && vUnit == null) ||
                            (unit != null && unit.equals(vUnit));

            boolean sameWeightAndUnit = sameWeight && sameUnit;
            boolean hasWeightUnit = weight != null && unit != null && !unit.isEmpty();

            // If supermarket is selected AND we actually know the weight+unit,
            // restrict to the same variant. Otherwise consider all variants.
            if (this.supermarket != null && hasWeightUnit && !sameWeightAndUnit)
            {
                continue;
            }

            double currPrice = row.getPrice();

            if (cheapestRow == null)
            {
                cheapestRow = row;
                cheapestPrice = currPrice;
                continue;
            }

            boolean hasCurrentCompany = this.company != null && !this.company.isEmpty();

            int currScoreCompany = (hasCurrentCompany && Objects.equals(this.company, info.getCompany())) ? 1 : 0;
            ItemInfo bestInfo = cheapestRow.getInfo();
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
                cheapestRow = row;
                cheapestPrice = currPrice;
            }
        }

        if (cheapestPrice < price || ogRow == null)
        {
            return cheapestRow;
        }

        return ogRow;
    }

    @Exclude
    public void setSupermarketRow(
            Supermarket supermarket,
            ArrayList<ItemViewPricesAdapter.PriceRow> rows)
    {
        ItemViewPricesAdapter.PriceRow row = getSupermarketRow(supermarket, rows);

        if (row != null)
        {
            fillRow(row);
        }
        else
        {
            setUnchosen();
        }
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
    public Supermarket getCheapestSupermarket(Map<Supermarket, Double> prices)
    {
        double cheapestPrice = this.price;
        Supermarket cheapestSupermarket = this.supermarket;

        if (prices == null || prices.isEmpty())
        {
            return this.supermarket;
        }

        if (this.supermarket == Baskit.UNASSIGNED_SUPERMARKET || this.price == 0.0)
        {
            cheapestPrice = Double.MAX_VALUE;
        }

        for (Supermarket supermarket : prices.keySet())
        {
            double price = prices.get(supermarket);

            if (price < cheapestPrice)
            {
                cheapestPrice = price;
                cheapestSupermarket = supermarket;
            }
        }

        return cheapestSupermarket;
    }

    @Exclude
    public void setCheapestSupermarketFromSmMap(Map<Supermarket, Double> prices)
    {
        Supermarket cheapest = getCheapestSupermarket(prices);
        this.supermarket = cheapest;

        if (prices != null)
        {
            Double newPrice = prices.get(cheapest);
            if (newPrice != null)
            {
                this.price = newPrice;
            }
        }
    }

    @Exclude
    public void setCheapestSupermarketFromStringsMap(Map<String, Map<String, Double>> prices)
    {
        setCheapestSupermarketFromSmMap(Supermarket.getSupermarketsPricesFromStrings(prices));
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
    public void fillRow(ItemViewPricesAdapter.PriceRow row)
    {
        fillInfo(row.getInfo());
        this.price = row.getPrice();
        this.supermarket = row.getSupermarket();
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
    public boolean isVariantOf(ItemViewPricesAdapter.PriceRow row)
    {
        ItemInfo info = row.getInfo();

        boolean sameCompany = java.util.Objects.equals(company, info.getCompany());

        Double thisWeight = this.getWeight();
        Double infoWeight = info.getWeight();
        boolean sameWeight =
                (thisWeight == null && infoWeight == null) ||
                (thisWeight != null && infoWeight != null && Double.compare(thisWeight, infoWeight) == 0);

        String infoUnit = info.getUnit();
        boolean sameUnit =
                (unit == null && infoUnit == null) ||
                (unit != null && unit.equals(infoUnit));

        boolean sameBaseName = java.util.Objects.equals(this.baseName, info.getBaseName());
        boolean sameSupermarket =
                row.getSupermarket() != null &&
                        this.supermarket != null &&
                        row.getSupermarket().getSupermarket().equals(this.supermarket.getSupermarket()) &&
                        row.getSupermarket().getSection().equals(this.supermarket.getSection());
        boolean samePrice = Double.compare(row.getPrice(), this.price) == 0;

        if (!sameBaseName || !sameSupermarket || !samePrice)
        {
            return false;
        }

        return sameCompany && sameWeight && sameUnit && sameSupermarket;
    }

    @Exclude
    public boolean hasCompany()
    {
        return company != null && !company.isEmpty() && !company.equals("unknown");
    }

    @Exclude
    public boolean hasWeight()
    {
        return weight != null && weight > 0;
    }

    @Exclude
    public boolean hasUnit()
    {
        return unit != null && !unit.isEmpty() && !unit.equals("unknown");
    }

    @Exclude
    public boolean isVariant()
    {
        return hasCompany() || (hasWeight() && hasUnit()) && hasSupermarket();
    }
}