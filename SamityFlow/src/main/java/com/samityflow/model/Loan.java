package com.samityflow.model;
public class Loan {
 private int id, applicationId, memberId;
 private double amount, outstanding;
 public Loan(){}
 public Loan(int id,int applicationId,int memberId,double amount){this.id=id;this.applicationId=applicationId;this.memberId=memberId;this.amount=amount;this.outstanding=amount;}
 public int getId(){return id;} public int getApplicationId(){return applicationId;} public int getMemberId(){return memberId;}
 public double getAmount(){return amount;} public double getOutstanding(){return outstanding;} public void setOutstanding(double v){outstanding=v;}
}
