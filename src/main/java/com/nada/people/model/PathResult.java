package com.nada.people.model;

import java.util.List;

public class PathResult {

    private boolean connected;
    private int hops;
    private List<Person> path;


    public PathResult(boolean connected,int hops,List<Person> path){
        this.connected=connected;
        this.hops=hops;
        this.path=path;
    }

    public boolean isConnected(){
        return connected;
    }

    public int getHops(){
        return hops;
    }

    public List<Person> getPath(){
        return path;
    }

}
