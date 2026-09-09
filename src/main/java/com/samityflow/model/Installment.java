package com.samityflow.model;
public class Installment {
 private int id,loanId,number; private double amount,paid;
 public Installment(){}
 public Installment(int id,int loanId,int number,double amount){this.id=id;this.loanId=loanId;this.number=number;this.amount=amount;}
 public int getId(){return id;} public int getLoanId(){return loanId;} public double getAmount(){return amount;} public double getPaid(){return paid;}
 public void setPaid(double p){paid=p;}
}
