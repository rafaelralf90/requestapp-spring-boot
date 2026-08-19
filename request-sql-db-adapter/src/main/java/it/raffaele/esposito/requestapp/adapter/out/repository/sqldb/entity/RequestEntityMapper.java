package it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity;

import it.raffaele.esposito.requestapp.request.domain.Request;
import it.raffaele.esposito.requestapp.request.domain.RequestStatus;

import java.time.Clock;

public final class RequestEntityMapper {

    private RequestEntityMapper() {
    }

    public static RequestEntity toEntity(Request request) {
        return new RequestEntity(
                request.getUuid(),
                request.getName(),
                request.getBody(),
                request.getStatus().name(),
                request.getPublishedRequestUuid(),
                request.getCreatedAt(),
                request.getDisabledDate(),
                request.getReason(),
                request.getVersion());
    }

    public static Request toDomain(RequestEntity requestEntity, Clock clock) {
        return Request.reconstitute(
                requestEntity.getUuid(),
                requestEntity.getName(),
                requestEntity.getBody(),
                RequestStatus.valueOf(requestEntity.getStatus()),
                requestEntity.getPublishedRequestUuid(),
                requestEntity.getCreatedAt(),
                requestEntity.getDisabledDate(),
                requestEntity.getReason(),
                requestEntity.getVersion(),
                clock);
    }
}
