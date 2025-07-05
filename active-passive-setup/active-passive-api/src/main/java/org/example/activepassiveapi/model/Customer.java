package org.example.activepassiveapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(staticName = "create")
public class Customer {
    private String id;
    private String name;
    private String email;
    private String phone;
}

