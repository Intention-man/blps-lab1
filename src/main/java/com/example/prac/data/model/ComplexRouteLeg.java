package com.example.prac.data.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplexRouteLeg {
    private City departureCity;
    private LocalDate departureDate;
    private LocalTime departureTimeStart;
    private LocalTime departureTimeFinish;
    private City arrivalCity;
    private LocalTime arrivalTimeStart;
    private LocalTime arrivalTimeFinish;
}
