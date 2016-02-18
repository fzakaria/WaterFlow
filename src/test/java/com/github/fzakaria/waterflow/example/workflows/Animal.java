package com.github.fzakaria.waterflow.example.workflows;

import com.google.common.base.Preconditions;
import lombok.Data;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A simple POJO to demonstrate passing complex types for Activities.
 */
@Data
public class Animal {
    private UUID id;
    private Double weight;
    private LocalDateTime dob;
    private Animal mother;
    private Animal father;

    public Animal() {
        this.id = UUID.randomUUID();
    }

    public static Animal mate(Animal mother, Animal father) {
        Preconditions.checkArgument(mother != father, "You cannot mate with yourself.");
        Animal a = new  Animal();
        a.weight = new SecureRandom().nextDouble()*100;
        a.dob = LocalDateTime.now();
        a.mother = mother;
        a.father = father;
        return a;
    }

}