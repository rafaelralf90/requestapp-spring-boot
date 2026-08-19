package it.raffaele.esposito.requestapp.request.ports.out.outbox;

import it.raffaele.esposito.requestapp.request.domain.event.RequestEvent;

import java.util.List;

public interface RequestEventOutbox {

    void append(List<RequestEvent> events);
}
