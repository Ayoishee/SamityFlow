package com.samityflow.model;
public class SavingsTransaction {
 private int id,memberId; private double amount; private String type;
 public SavingsTransaction(int id,int memberId,double amount,String type){this.id=id;this.memberId=memberId;this.amount=amount;this.type=type;}
 public int getId(){return id;} public int getMemberId(){return memberId;} public double getAmount(){return amount;} public String getType(){return type;}
}
