package com.smartwarehouse;
public class Product{
 public int id,stock,reorderLevel; public String name,category; public double price;
 public Product(int id,String name,String category,double price,int stock,int reorderLevel){this.id=id;this.name=name;this.category=category;this.price=price;this.stock=stock;this.reorderLevel=reorderLevel;}
}