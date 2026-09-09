package com.samityflow.model;


public class Payment {

    private int id;

    private int loanId;

    private int installmentId;

    private int memberId;

    private double amount;

    private String reference;

    private String status;



    public Payment() {

    }



    public Payment(
            int id,
            int loanId,
            int installmentId,
            int memberId,
            double amount,
            String reference,
            String status
    ){

        this.id = id;
        this.loanId = loanId;
        this.installmentId = installmentId;
        this.memberId = memberId;
        this.amount = amount;
        this.reference = reference;
        this.status = status;

    }



    public int getId(){
        return id;
    }


    public void setId(int id){
        this.id=id;
    }



    public int getLoanId(){
        return loanId;
    }


    public void setLoanId(int loanId){
        this.loanId=loanId;
    }



    public int getInstallmentId(){
        return installmentId;
    }


    public void setInstallmentId(int installmentId){
        this.installmentId=installmentId;
    }



    public int getMemberId(){
        return memberId;
    }


    public void setMemberId(int memberId){
        this.memberId=memberId;
    }



    public double getAmount(){
        return amount;
    }


    public void setAmount(double amount){
        this.amount=amount;
    }



    public String getReference(){
        return reference;
    }


    public void setReference(String reference){
        this.reference=reference;
    }



    public String getStatus(){
        return status;
    }


    public void setStatus(String status){
        this.status=status;
    }

}