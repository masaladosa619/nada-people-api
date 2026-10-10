package com.nada.people.controller;
import org.springframework.web.bind.annotation.RestController;

import com.nada.people.model.ConnectPeopleRequest;
import com.nada.people.model.CreatePersonRequest;
import com.nada.people.model.PathResult;
import com.nada.people.model.Person;
import com.nada.people.service.PeopleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;




@RestController 
@RequiredArgsConstructor 
public class PeopleController {
    
    private final PeopleService peopleService;

    @PostMapping("/people")
    @ResponseStatus(HttpStatus.CREATED)
    public Person CreatePersonRequest(@Valid @RequestBody CreatePersonRequest request) {
        return peopleService.addPerson(request.name());
    }
    
    @PostMapping("/knows")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ConnectPeopleRequest(@Valid @RequestBody ConnectPeopleRequest request) {
        peopleService.connect(request.person1(), request.person2());
    }

    @GetMapping("/people/{id}/contacts")
    public List<Person> getContacts(@PathVariable int id) {
        return peopleService.getContacts(id);
    }
    

    @GetMapping("/path")
    public PathResult findPath(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }

        int fromId;
        int toId;

        try {
            fromId = Integer.parseInt(from);
            toId = Integer.parseInt(to);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "from and to must be integers"
            );
        }

        return peopleService.findPath(fromId, toId);
    }


    
    @GetMapping("/test")
    public String test() {
        return "people API is working";
    }
    
    

}
