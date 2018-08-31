package com.security.ssms;

import java.util.Comparator;
import java.util.HashMap;

//Class that is used mostly for comparing HashMaps
class MapComparator implements Comparator<HashMap<String, String>>
{
    //Create class attributes
    private final String key;
    private final String order;

    //Two-arg constructor
    public MapComparator(String key, String order)
    {
        this.key = key;
        this.order = order;
    }

    //Method for comparing two HashMaps
    public int compare(HashMap<String, String> first, HashMap<String, String> second)
    {
        String firstValue = first.get(key);
        String secondValue = second.get(key);
        if(this.order.toLowerCase().contentEquals("asc"))
        {
            return firstValue.compareTo(secondValue);
        }
        else{
            return secondValue.compareTo(firstValue);
        }

    }
}