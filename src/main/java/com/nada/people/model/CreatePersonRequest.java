
package com.nada.people.model;

import jakarta.validation.constraints.NotBlank;

public record CreatePersonRequest(
        @NotBlank(message = "name cannot be blank") String name) {
}
