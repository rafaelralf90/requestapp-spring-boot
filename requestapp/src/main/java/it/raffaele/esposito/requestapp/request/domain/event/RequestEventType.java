package it.raffaele.esposito.requestapp.request.domain.event;

import it.raffaele.esposito.requestapp.request.domain.RequestStatus;

public enum RequestEventType {

    CREATED,
    BODY_UPDATED,
    VERIFIED,
    ACCEPTED,
    REJECTED,
    PUBLISHED,
    DELETED;

    public static RequestEventType arrivingAt(RequestStatus status) {
        return switch (status) {
            case VERIFIED -> VERIFIED;
            case ACCEPTED -> ACCEPTED;
            case REJECTED -> REJECTED;
            case PUBLISHED -> PUBLISHED;
            case DELETED -> DELETED;
            case CREATED -> throw new IllegalArgumentException(
                    "A request is created rather than transitioned into " + status);
        };
    }
}
