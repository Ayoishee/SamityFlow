package com.samityflow.model;
public class AuditLog {
 private int id; private String action;
 public AuditLog(int id,String action){this.id=id;this.action=action;}
 public int getId(){return id;} public String getAction(){return action;}
}
