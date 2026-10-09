package com.nada.people.service;

import java.util.HashMap;
import java.util.Map;

import com.nada.people.model.Person;


public class PeopleService {
    private final Map<Integer,Person> people = new HashMap<>();
    private int nextId = 1;
    
    public Person addPerson(String name){
        
        Person p = new Person(nextId++, name);
        people.put(p.getId(), p);

        return p;
    }
}
 