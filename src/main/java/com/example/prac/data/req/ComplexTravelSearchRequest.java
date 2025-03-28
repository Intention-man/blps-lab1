package com.example.prac.data.req;

import lombok.Data;

import java.util.List;

@Data
public class ComplexTravelSearchRequest {
    private Integer passengerCount;
    private String serviceClass;
    private int maxPrice;
    private int maxTravelTime;
    private int numberOfTransfers;
    private List<String> availableAirlines;
    private List<ComplexRouteLeg> routes;
}
