package com.nada.people.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConnectPeopleRequest(
        @NotNull(message = "person1 is required")
        @Positive(message = "person1 must be a positive integer")
        Integer person1,

        @NotNull(message = "person2 is required")
        @Positive(message = "person2 must be a positive integer")
        Integer person2
) {
}
