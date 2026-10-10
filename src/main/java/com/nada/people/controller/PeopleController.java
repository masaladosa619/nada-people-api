package com.nada.people.controller;
import org.springframework.web.bind.annotation.RestController;

import com.nada.people.model.ConnectPeopleRequest;
import com.nada.people.model.CreatePersonRequest;
import com.nada.people.model.PathResult;
import com.nada.people.model.Person;
import com.nada.people.service.PeopleService;
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
    public Person CreatePersonRequest(@RequestBody CreatePersonRequest request) {        
        return peopleService.addPerson(request.name());
    }
    
    @PostMapping("/knows")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ConnectPeopleRequest(@RequestBody ConnectPeopleRequest request) {
        peopleService.connect(request.person1(), request.person2());
    }

    @GetMapping("/people/{id}/contacts")
    public List<Person> getContacts(@PathVariable int id) {
        return peopleService.getContacts(id);
    }
    
    @GetMapping("/path")
    public PathResult findPath(@RequestParam int from,@RequestParam int to) {
        return peopleService.findPath(from, to);
    }
    @GetMapping("/test")
    public String test() {
        return "people API is working";
    }
    
    

}
