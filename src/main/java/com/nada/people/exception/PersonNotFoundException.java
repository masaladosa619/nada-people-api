package com.nada.people.exception;

public class PersonNotFoundException extends RuntimeException{
    public PersonNotFoundException(int id){
        super("no person " + id);
    }
}
