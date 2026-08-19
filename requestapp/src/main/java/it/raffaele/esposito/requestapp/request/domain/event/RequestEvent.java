package it.raffaele.esposito.requestapp.request.domain.event;

import it.raffaele.esposito.requestapp.request.domain.RequestStatus;

import java.time.Instant;

public record RequestEvent(String requestUuid,
                           RequestEventType type,
                           RequestStatus fromStatus,
                           RequestStatus toStatus,
                           String reason,
                           String publishedRequestUuid,
                           long decidedOnVersion,
                           Instant occurredAt) {
}
