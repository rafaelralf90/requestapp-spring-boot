package it.raffaele.esposito.requestapp.request.domain;

import it.raffaele.esposito.requestapp.request.domain.event.RequestEvent;
import it.raffaele.esposito.requestapp.request.domain.event.RequestEventType;
import it.raffaele.esposito.requestapp.request.domain.exceptions.FieldTooLongException;
import it.raffaele.esposito.requestapp.request.domain.exceptions.MandatoryDataMissingException;
import it.raffaele.esposito.requestapp.request.domain.exceptions.OperationNotAllowedInCurrentState;
import it.raffaele.esposito.requestapp.request.domain.exceptions.RequestStateTransitionNotAllowed;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
public class Request {

    public static final long INITIAL_VERSION = 0L;

    public static final int NAME_MAX_LENGTH = 255;

    public static final int BODY_MAX_LENGTH = 4000;

    public static final int REASON_MAX_LENGTH = 4000;

    private static final Set<RequestStatus> BODY_EDITABLE_IN =
            EnumSet.of(RequestStatus.CREATED, RequestStatus.VERIFIED);

    private final String uuid;

    private final String name;

    private String body;

    private RequestStatus status;

    private String publishedRequestUuid;

    private final Instant createdAt;

    private Instant disabledDate;

    private String reason;

    private long version;

    @Getter(AccessLevel.NONE)
    private final List<RequestEvent> pendingEvents = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private final Clock clock;

    public Request(String name, String body) {
        this(name, body, Clock.systemUTC());
    }

    public Request(String name, String body, Clock clock) {
        this(UUID.randomUUID().toString(), required(name, "name", NAME_MAX_LENGTH),
                required(body, "body", BODY_MAX_LENGTH),
                RequestStatus.CREATED, null, Instant.now(clock), null, null, INITIAL_VERSION, clock);
        raise(RequestEventType.CREATED, null);
    }

    private Request(String uuid, String name, String body, RequestStatus status, String publishedRequestUuid,
                    Instant createdAt, Instant disabledDate, String reason, long version, Clock clock) {
        this.uuid = uuid;
        this.name = name;
        this.body = body;
        this.status = status;
        this.publishedRequestUuid = publishedRequestUuid;
        this.createdAt = createdAt;
        this.disabledDate = disabledDate;
        this.reason = reason;
        this.version = version;
        this.clock = clock;
    }

    public void updateBody(String body) {
        if (!BODY_EDITABLE_IN.contains(this.status)) {
            throw new OperationNotAllowedInCurrentState(
                    "A request body can only be updated while it is one of " + BODY_EDITABLE_IN
                            + ", current status: " + this.status);
        }
        this.body = required(body, "body", BODY_MAX_LENGTH);
        raise(RequestEventType.BODY_UPDATED, this.status);
    }

    private static String required(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new MandatoryDataMissingException(fieldName);
        }
        if (value.length() > maxLength) {
            throw new FieldTooLongException(fieldName, maxLength);
        }
        return value;
    }

    public void verify() {
        final RequestStatus from = transitionTo(RequestStatus.VERIFIED, EnumSet.of(RequestStatus.CREATED));
        raise(RequestEventType.arrivingAt(this.status), from);
    }

    public void accept() {
        final RequestStatus from = transitionTo(RequestStatus.ACCEPTED, EnumSet.of(RequestStatus.VERIFIED));
        raise(RequestEventType.arrivingAt(this.status), from);
    }

    public void reject(String reason) {
        final String rejectionReason = required(reason, "reason", REASON_MAX_LENGTH);
        final RequestStatus from =
                transitionTo(RequestStatus.REJECTED, EnumSet.of(RequestStatus.VERIFIED, RequestStatus.ACCEPTED));
        this.reason = rejectionReason;
        raise(RequestEventType.arrivingAt(this.status), from);
    }

    public String publish() {
        final RequestStatus from = transitionTo(RequestStatus.PUBLISHED, EnumSet.of(RequestStatus.ACCEPTED));
        this.publishedRequestUuid = UUID.randomUUID().toString();
        raise(RequestEventType.arrivingAt(this.status), from);
        return this.publishedRequestUuid;
    }

    public void delete(String reason) {
        final String deletionReason = required(reason, "reason", REASON_MAX_LENGTH);
        final RequestStatus from = transitionTo(RequestStatus.DELETED, EnumSet.of(RequestStatus.CREATED));
        this.disabledDate = Instant.now(this.clock);
        this.reason = deletionReason;
        raise(RequestEventType.arrivingAt(this.status), from);
    }

    private RequestStatus transitionTo(RequestStatus target, Set<RequestStatus> allowedSources) {
        if (!allowedSources.contains(this.status)) {
            throw new RequestStateTransitionNotAllowed(
                    "A request cannot move from " + this.status + " to " + target);
        }
        final RequestStatus previousStatus = this.status;
        this.status = target;
        return previousStatus;
    }

    private void raise(RequestEventType type, RequestStatus fromStatus) {
        this.pendingEvents.add(new RequestEvent(this.uuid, type, fromStatus, this.status, this.reason,
                this.publishedRequestUuid, this.version, Instant.now(this.clock)));
    }

    public List<RequestEvent> pullEvents() {
        final List<RequestEvent> raisedSoFar = List.copyOf(this.pendingEvents);
        this.pendingEvents.clear();
        return raisedSoFar;
    }

    public static Request reconstitute(String uuid, String name, String body, RequestStatus status,
                                       String publishedRequestUuid, Instant createdAt, Instant disabledDate,
                                       String reason, long version) {
        return reconstitute(uuid, name, body, status, publishedRequestUuid, createdAt, disabledDate, reason, version,
                Clock.systemUTC());
    }

    public static Request reconstitute(String uuid, String name, String body, RequestStatus status,
                                       String publishedRequestUuid, Instant createdAt, Instant disabledDate,
                                       String reason, long version, Clock clock) {
        return new Request(uuid, name, body, status, publishedRequestUuid, createdAt, disabledDate, reason, version,
                clock);
    }

    public void writtenAsVersion(long version) {
        this.version = version;
    }

    public boolean isAtVersion(long version) {
        return this.version == version;
    }
}
