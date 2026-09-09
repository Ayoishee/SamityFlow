package com.samityflow.pattern.factory;
import java.util.*;
public class ScheduleFactory {
 public List<Double> createWeekly(double amount,int weeks){
  List<Double> list=new ArrayList<>(); for(int i=0;i<weeks;i++) list.add(amount/weeks); return list;
 }
}
