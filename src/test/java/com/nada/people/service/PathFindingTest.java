package com.nada.people.service;

import com.nada.people.model.PathResult;
import com.nada.people.model.Person;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathFindingTest {

    @Test
    void threeHopsIsConnected() {
        // Setup: A - B - C - D
        PeopleService service = new PeopleService();

        Person a = service.addPerson("A");
        Person b = service.addPerson("B");
        Person c = service.addPerson("C");
        Person d = service.addPerson("D");

        service.connect(a.getId(), b.getId());
        service.connect(b.getId(), c.getId());
        service.connect(c.getId(), d.getId());

        // Action: Find path from A to D
        PathResult result = service.findPath(a.getId(), d.getId());

        // Assert: Exactly 3 hops should be connected
        assertTrue(result.isConnected());
        assertEquals(3, result.getHops());
    }

    @Test
    void moreThanThreeHopsIsNotConnected() {
        // Setup: A - B - C - D - E
        PeopleService service = new PeopleService();

        Person a = service.addPerson("A");
        Person b = service.addPerson("B");
        Person c = service.addPerson("C");
        Person d = service.addPerson("D");
        Person e = service.addPerson("E");

        service.connect(a.getId(), b.getId());
        service.connect(b.getId(), c.getId());
        service.connect(c.getId(), d.getId());
        service.connect(d.getId(), e.getId());

        // Action: Find path from A to E
        PathResult result = service.findPath(a.getId(), e.getId());

        // Assert: More than 3 hops should not be connected
        assertFalse(result.isConnected());
    }
}
