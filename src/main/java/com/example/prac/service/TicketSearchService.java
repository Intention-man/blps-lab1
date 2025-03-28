package com.example.prac.service;

import com.example.prac.data.model.Route;
import com.example.prac.data.model.SimpleTravelSearchRequest;
import com.example.prac.data.model.Ticket;
import com.example.prac.data.req.SimpleTravelSearchRequestDTO;
import com.example.prac.data.res.RouteDTO;
import com.example.prac.data.res.TravelVariantDTO;
import com.example.prac.mappers.RouteMapper;
import com.example.prac.mappers.SimpleTravelSearchRequestMapper;
import com.example.prac.repository.TicketRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class TicketSearchService {
    private SimpleTravelSearchRequestMapper simpleReqMapper;
    private TicketRepository ticketRepository;
    private RouteMapper routeMapper;

    public List<TravelVariantDTO> searchSimpleRoutes(SimpleTravelSearchRequestDTO simpleReqDTO, boolean needBackTickets) {
        SimpleTravelSearchRequest req = simpleReqMapper.mapFrom(simpleReqDTO);
        List<Route> simpleRouteVariants = new ArrayList<>();
        findAndSetSimpleRouteVariants(req, simpleRouteVariants);
        if (needBackTickets) {
            List<TravelVariantDTO> result = new ArrayList<>();
            for (Route route : simpleRouteVariants) {
                SimpleTravelSearchRequest reqBack = simpleReqMapper.mapFrom2(simpleReqDTO, route);
                List<Route> simpleRouteVariantsBack = new ArrayList<>();
                findAndSetSimpleRouteVariants(reqBack, simpleRouteVariantsBack);
                for (Route routeBack : simpleRouteVariantsBack) {
                    RouteDTO r1 = routeMapper.mapTo(route);
                    RouteDTO r2 = routeMapper.mapTo(routeBack);
                    result.add(new TravelVariantDTO(
                            route.getTotalPrice() + routeBack.getTotalPrice(),
                            List.of(r1, r2)
                    ));
                }
            }
            return result;
        } else {
            return simpleRouteVariants.stream()
                    .map(routeMapper::mapTo)
                    .map(routeDTO -> new TravelVariantDTO(routeDTO.getTotalPrice(), List.of(routeDTO)))
                    .toList();
        }
    }

    private void findAndSetSimpleRouteVariants(SimpleTravelSearchRequest req, List<Route> simpleRouteVariants) {
        List<Ticket> firstTicketCandidates = ticketRepository.findFirstTickets(
                req.getServiceClass(), req.getPassengerCount(), req.getMaxPrice(), req.getMaxTravelTime(), req.getAvailableAirlines(),
                req.getDepartureCity(), req.getDepartureDateStart(), req.getDepartureDateFinish(),
                req.getDepartureTimeStart(), req.getDepartureTimeFinish());

        for (Ticket ticket : firstTicketCandidates) {
            if (ticket.getArrivalCity().equals(req.getArrivalCity())) {
                if (isSuitableFinishTicket(ticket, req)) {
                    Route route = initRouteWithFirstTicket(ticket, req);
                    simpleRouteVariants.add(route);
                }
            } else if (canTicketBeIncludeInRoute(ticket, req)) {
                Route route = initRouteWithFirstTicket(ticket, req);
                nextStep(req, route, req.getNumberOfTransfers() - 1, simpleRouteVariants);
            }
        }
    }

    private void nextStep(SimpleTravelSearchRequest req, Route route, int leftNumberOfTransfers, List<Route> simpleRouteVariants) {
        if (leftNumberOfTransfers == 0)
            return;

        Ticket lastTicket = route.getTickets().get(route.getTickets().size() - 1);

        if (leftNumberOfTransfers == 1) {
            LocalDate departureDateStart = lastTicket.getArrivalDate().isAfter(req.getArrivalDateStart()) ?
                    lastTicket.getArrivalDate() :
                    req.getArrivalDateStart();

            List<Ticket> nextTicketCandidates = ticketRepository.findFinishTickets(
                    req.getServiceClass(),
                    req.getPassengerCount(),
                    req.getMaxPrice() - route.getTotalPrice(),
                    req.getMaxTravelTime() - route.getTotalHours(),
                    req.getAvailableAirlines(),
                    lastTicket.getArrivalCity(),
                    departureDateStart,
                    req.getDepartureDateFinish(),
                    LocalTime.MIN,
                    LocalTime.of(23, 59, 59),
                    req.getArrivalCity(),
                    req.getArrivalDateStart(),
                    req.getArrivalDateFinish(),
                    req.getArrivalTimeStart(),
                    req.getArrivalTimeFinish(),
                    route.getMaxFinishDatetime()
            );

            for (Ticket ticket : nextTicketCandidates) {
                Route updatedRoute = cloneRouteAddingTicket(route, ticket);
                simpleRouteVariants.add(updatedRoute);
            }
        } else {
            List<Ticket> nextTicketCandidates = ticketRepository.findIntermediateTickets(
                    req.getServiceClass(),
                    req.getPassengerCount(),
                    req.getMaxPrice() - route.getTotalPrice(),
                    req.getMaxTravelTime() - route.getTotalHours(),
                    req.getAvailableAirlines(),
                    lastTicket.getArrivalCity(),
                    lastTicket.getArrivalDateTime(),
                    route.getMaxFinishDatetime());

            for (Ticket ticket : nextTicketCandidates) {
                if (ticket.getArrivalCity().equals(req.getArrivalCity())) {
                    if (isSuitableFinishTicket(ticket, req)) {
                        Route updatedRoute = cloneRouteAddingTicket(route, ticket);
                        simpleRouteVariants.add(updatedRoute);
                    }
                } else if (canTicketBeIncludeInRoute(ticket, req)) {
                    Route updatedRoute = cloneRouteAddingTicket(route, ticket);
                    nextStep(req, updatedRoute, leftNumberOfTransfers - 1, simpleRouteVariants);
                }
            }
        }
    }

    private boolean isSuitableFinishTicket(Ticket ticket, SimpleTravelSearchRequest req) {
        return ticket.getArrivalCity().equals(req.getArrivalCity()) &&
                ticket.getArrivalTime().isAfter(req.getArrivalTimeStart()) &&
                ticket.getArrivalTime().isBefore(req.getArrivalTimeFinish()) &&
                ticket.getArrivalDate().isAfter(req.getArrivalDateStart()) &&
                ticket.getArrivalDate().isBefore(req.getArrivalDateFinish());
    }

    private boolean canTicketBeIncludeInRoute(Ticket ticket, SimpleTravelSearchRequest req) {
        return ticket.getArrivalTime().isBefore(req.getArrivalTimeFinish()) &&
                ticket.getArrivalDate().isBefore(req.getArrivalDateFinish());
    }

    private Route initRouteWithFirstTicket(Ticket ticket, SimpleTravelSearchRequest req) {
        Route route = new Route();
        route.setDepartureCity(req.getDepartureCity());
        route.setArrivalCity(req.getArrivalCity());
        route.setTotalHours(ticket.getHours());
        route.setTotalPrice(ticket.getPrice());
        route.setMaxFinishDatetime(calcMaxFinishDatetime(ticket, req));
        List<Ticket> list = new ArrayList<>();
        list.add(ticket);
        route.setTickets(list);
        return route;
    }

    private Route cloneRouteAddingTicket(Route route, Ticket ticket) {
        Route updatedRoute = new Route();
        updatedRoute.setDepartureCity(route.getDepartureCity());
        updatedRoute.setArrivalCity(ticket.getArrivalCity());

        double additionalTransferDuration = calcTransferDurationInHours(route.getTickets().get(route.getTickets().size() - 1), ticket);
        updatedRoute.setTotalHours(route.getTotalHours() + additionalTransferDuration);

        updatedRoute.setTotalPrice(route.getTotalPrice() + ticket.getPrice());
        updatedRoute.setMaxFinishDatetime(route.getMaxFinishDatetime());

        //NOTE учитывая что Ticket - (по логике программы) неизменяемый объект, думаю, что можно просто копировать ссылки, а не делать глубокое копирование
        List<Ticket> list = new ArrayList<>(route.getTickets());
        list.add(ticket);
        updatedRoute.setTickets(list);
        return updatedRoute;
    }

    private LocalDateTime calcMaxFinishDatetime(Ticket ticket, SimpleTravelSearchRequest req) {
        return ticket.getDepartureDateTime().plusHours(req.getMaxTravelTime());
    }

    public double calcTransferDurationInHours(Ticket ticket1, Ticket ticket2) {
        return java.time.Duration.between(ticket1.getArrivalDateTime(), ticket2.getDepartureDateTime()).toMinutes() / 60.0;
    }
}