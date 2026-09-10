package com.samityflow.service;
import java.util.*;
public class RepaymentService {
 public List<Double> generateWeeklySchedule(double amount,int weeks){
  if(amount<=0) throw new IllegalArgumentException("Loan amount must be positive");
  if(weeks<=0) throw new IllegalArgumentException("Repayment weeks must be positive");
  List<Double> result=new ArrayList<>();
  double value=amount/weeks;
  for(int i=0;i<weeks;i++) result.add(value);
  return result;
 }
}
