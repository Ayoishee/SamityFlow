package com.samityflow.service;
public class SavingsService {
 public boolean validateWithdrawal(double balance,double amount){return amount>=0 && amount<=balance;}
}
