
package com.samityflow.service;

public class CollectionResult {
    private final boolean success;
    private final String message;

    public CollectionResult(boolean success,String message){
        this.success=success;
        this.message=message;
    }

    public boolean isSuccess(){return success;}
    public String getMessage(){return message;}
}
