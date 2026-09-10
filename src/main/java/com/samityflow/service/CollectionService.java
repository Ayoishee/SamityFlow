package com.samityflow.service;
import java.util.*;
public class CollectionService {
 private final Set<String> references=new HashSet<>();
 public boolean collect(double amount,String reference){
  if(amount<=0 || reference==null || reference.isBlank() || references.contains(reference)) return false;
  references.add(reference); return true;
 }
 public boolean reverse(String reference){return references.remove(reference);}
}
