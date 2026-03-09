package com.example.baskit.MainComponents;

import java.util.Objects;

public class ItemInfo
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

    public void setCode(String code) {
        this.code = code;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getCompany() {
        return company;
    }

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

        if (weight.doubleValue() == weight.longValue())
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

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getUnit() {
        return unit;
    }

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

        return Objects.equals(baseName, other.baseName) &&
                Objects.equals(company, other.company) &&
                Objects.equals(weight, other.weight) &&
                Objects.equals(unit, other.unit);
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

            if (unit != null && !unit.isEmpty() && !unit.equals("יחידות"))
            {
                str += " " + unit;
            }
        }

        return str;
    }
}
