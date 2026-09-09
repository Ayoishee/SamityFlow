package com.samityflow.model;
public class Payment {
 private int id,loanId; private double amount; private String reference;
 public Payment(){}
 public Payment(int id,int loanId,double amount,String reference){this.id=id;this.loanId=loanId;this.amount=amount;this.reference=reference;}
 public int getId(){return id;} public int getLoanId(){return loanId;} public double getAmount(){return amount;} public String getReference(){return reference;}
}
