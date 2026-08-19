package it.raffaele.esposito.requestapp.request.ports.in.dto.out;

import it.raffaele.esposito.requestapp.request.domain.Request;
import it.raffaele.esposito.requestapp.request.domain.RequestStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class RequestData {

    private final String uuid;
    private final String name;
    private final String body;
    private final RequestStatus status;
    private final String publishedRequestUuid;
    private final String reason;
    private final Instant createdAt;
    private final Instant disabledDate;
    private final long version;

    public static RequestData from(Request request) {
        return new RequestData(request.getUuid(), request.getName(), request.getBody(), request.getStatus(),
                request.getPublishedRequestUuid(), request.getReason(), request.getCreatedAt(),
                request.getDisabledDate(), request.getVersion());
    }
}
