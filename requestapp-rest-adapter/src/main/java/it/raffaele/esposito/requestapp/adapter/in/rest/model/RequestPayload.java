package it.raffaele.esposito.requestapp.adapter.in.rest.model;

import it.raffaele.esposito.requestapp.request.domain.RequestStatus;
import it.raffaele.esposito.requestapp.request.ports.in.dto.out.RequestData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class RequestPayload {

    private final String uuid;
    private final String name;
    private final String body;
    private final RequestStatus status;
    private final String publishedRequestUuid;
    private final String reason;
    private final Instant createdAt;
    private final long version;

    public static RequestPayload from(RequestData requestData) {
        return new RequestPayload(requestData.getUuid(), requestData.getName(), requestData.getBody(),
                requestData.getStatus(), requestData.getPublishedRequestUuid(), requestData.getReason(),
                requestData.getCreatedAt(), requestData.getVersion());
    }
}
