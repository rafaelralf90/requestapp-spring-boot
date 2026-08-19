package it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity;

import it.raffaele.esposito.requestapp.request.domain.RequestStatus;
import it.raffaele.esposito.requestapp.request.domain.event.RequestEvent;
import it.raffaele.esposito.requestapp.request.domain.event.RequestEventType;

import java.time.Instant;

public final class RequestEventEntityMapper {

    private RequestEventEntityMapper() {
    }

    public static RequestEventEntity toEntity(RequestEvent event) {
        return new RequestEventEntity(null, event.requestUuid(), event.type().name(),
                event.fromStatus() == null ? null : event.fromStatus().name(), event.toStatus().name(),
                event.reason(), event.publishedRequestUuid(), event.decidedOnVersion(), event.occurredAt(), null);
    }

    public static RequestEvent toDomain(RequestEventEntity entity) {
        return new RequestEvent(entity.getRequestUuid(),
                RequestEventType.valueOf(entity.getEventType()),
                entity.getFromStatus() == null ? null : RequestStatus.valueOf(entity.getFromStatus()),
                RequestStatus.valueOf(entity.getToStatus()),
                entity.getReason(),
                entity.getPublishedRequestUuid(),
                entity.getDecidedOnVersion(),
                entity.getOccurredAt() == null ? null : Instant.from(entity.getOccurredAt()));
    }
}
