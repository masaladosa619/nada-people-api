
package com.nada.people;

import com.google.gson.Gson;
import com.nada.people.service.PeopleService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws IOException {
        PeopleService peopleService = new PeopleService();
        Gson gson = new Gson();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0);

        // Endpoints will be registered here.

        server.start();
        System.out.println("Server running on port 8080");
    }
}
