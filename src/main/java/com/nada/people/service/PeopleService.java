package com.nada.people.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.nada.people.model.PathResult;
import com.nada.people.model.Person;


public class PeopleService {
    private final Map<Integer,Person> people = new HashMap<>();
    private final Map<Integer, Set<Integer>> adjacency = new HashMap<>();
    private int nextId = 1;
    
    public Person addPerson(String name){
        Person p = new Person(nextId++, name);
        people.put(p.getId(),p);
        adjacency.put(p.getId(), new HashSet<>());
        return p;
    }

    public void connect(int a,int b){

        if(a==b){
            throw new IllegalArgumentException("A PERSON CANNOT KNOW THEMSELVES");
        }

        if(!people.containsKey(a) || !people.containsKey(b)){
            throw new IllegalArgumentException("Person does not exist");
        }

        adjacency.get(a).add(b);
        adjacency.get(b).add(a);
    }

    
    public List<Person> getContacts(int id) {
    if (!people.containsKey(id)) {
        throw new IllegalArgumentException("Person does not exist: " + id);
    }
    
    Set<Integer> contactIds = adjacency.get(id);
    List<Person> contacts = new ArrayList<>();

    for (int contactId : contactIds) {
        Person p = people.get(contactId);
        contacts.add(p);
    }
    return contacts;
}

public PathResult findPath(int from,int to){
    Queue<Integer> queue = new ArrayDeque<>();

    Map<Integer,Integer> parentOf = new HashMap<>();
    Map<Integer,Integer> depthOf = new HashMap<>();

    if (!people.containsKey(from)) {
        throw new IllegalArgumentException("Person does not exist: " + from);
    }
    if (!people.containsKey(to)) {
        throw new IllegalArgumentException("Person does not exist: " + to);
    }

    queue.add(from);
    depthOf.put(from,0);

    if (from == to) {
        return new PathResult(
                true,
                0,
                List.of(people.get(from)));
    }

    while (!queue.isEmpty()) {
        int current = queue.poll();
        if (depthOf.get(current) == 3) {
            continue;
        }
        Set<Integer> neighbours = adjacency.get(current);
        for(int neighbour : neighbours){
            if (depthOf.containsKey(neighbour)) { //node already present
                continue;
            }

            depthOf.put(neighbour, depthOf.get(current) + 1);
            parentOf.put(neighbour, current);

            if (neighbour == to) { //target reached
                List<Person> path = buildPath(from, to, parentOf);
                int hops = depthOf.get(to);

                return new PathResult(true,hops,path);
            }
            queue.add(neighbour);
        }
    }
    return new PathResult(false, 0, List.of());
}

private  List<Person> buildPath(int from,int to,Map<Integer,Integer>parentOf){
    LinkedList<Person> path = new LinkedList<>();
    int current = to;

    while(current!=from){
        path.addFirst(people.get(current));
        current=parentOf.get(current);
    }

    path.addFirst(people.get(from));
    return path;
}



}
 