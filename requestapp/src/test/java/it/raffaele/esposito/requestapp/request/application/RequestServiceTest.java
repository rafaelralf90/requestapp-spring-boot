package it.raffaele.esposito.requestapp.request.application;

import it.raffaele.esposito.requestapp.request.domain.Request;
import it.raffaele.esposito.requestapp.request.domain.RequestStatus;
import it.raffaele.esposito.requestapp.request.application.exceptions.InvalidCommandException;
import it.raffaele.esposito.requestapp.request.application.exceptions.RequestNotFoundException;
import it.raffaele.esposito.requestapp.request.domain.exceptions.MandatoryDataMissingException;
import it.raffaele.esposito.requestapp.request.domain.exceptions.OperationNotAllowedInCurrentState;
import it.raffaele.esposito.requestapp.request.domain.exceptions.RequestStateTransitionNotAllowed;
import it.raffaele.esposito.requestapp.request.ports.in.RequestServicePort;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.NewRequestCommand;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.RequestUpdateStatusWithReason;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.RequestUpdateBodyCommand;
import it.raffaele.esposito.requestapp.request.domain.event.RequestEvent;
import it.raffaele.esposito.requestapp.request.domain.event.RequestEventType;
import it.raffaele.esposito.requestapp.request.ports.in.dto.out.RequestData;
import it.raffaele.esposito.requestapp.request.ports.out.outbox.RequestEventOutbox;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestLookupScope;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestRepo;
import it.raffaele.esposito.requestapp.request.application.exceptions.StaleRequestVersionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;

class RequestServiceTest {

    private final InMemoryRequestRepo requestRepo = new InMemoryRequestRepo();
    private final InMemoryRequestEventOutbox outbox = new InMemoryRequestEventOutbox();
    private final RequestServicePort requestService = new RequestService(requestRepo, outbox, Clock.systemUTC());

    @Test
    void createRequestStoresTheRequestAndReturnsItsUuid() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));

        assertNotNull(uuid);
        final Request stored = storedRequest(uuid);
        assertNotNull(stored);
        assertEquals("first request", stored.getName());
        assertEquals("the body", stored.getBody());
        assertEquals(RequestStatus.CREATED, stored.getStatus());
        assertNotNull(stored.getCreatedAt());
    }

    @Test
    void getRequestByIdReturnsTheStoredRequest() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));

        final RequestData found = requestService.getRequestById(uuid);

        assertEquals(uuid, found.getUuid());
        assertEquals("first request", found.getName());
        assertEquals("the body", found.getBody());
        assertEquals(RequestStatus.CREATED, found.getStatus());
        assertNotNull(found.getCreatedAt());
        assertEquals(Request.INITIAL_VERSION, found.getVersion());
        assertNull(found.getPublishedRequestUuid());
        assertNull(found.getReason());
        assertNull(found.getDisabledDate());
    }

    @Test
    void walksARequestAllTheWayToPublished() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));

        requestService.verify(uuid, 0L);
        assertEquals(RequestStatus.VERIFIED, storedRequest(uuid).getStatus());

        requestService.accept(uuid, 1L);
        assertEquals(RequestStatus.ACCEPTED, storedRequest(uuid).getStatus());

        final String publishedRequestUuid = requestService.publish(uuid, 2L);
        assertNotNull(publishedRequestUuid);
        assertEquals(RequestStatus.PUBLISHED, storedRequest(uuid).getStatus());
        assertEquals(publishedRequestUuid, storedRequest(uuid).getPublishedRequestUuid());
    }

    @Test
    void aRejectedRequestKeepsTheReasonItWasRejectedFor() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));
        requestService.verify(uuid, 0L);

        requestService.reject(new RequestUpdateStatusWithReason(uuid, "does not meet the guidelines"), 1L);

        assertEquals(RequestStatus.REJECTED, storedRequest(uuid).getStatus());
        assertEquals("does not meet the guidelines", storedRequest(uuid).getReason());
    }

    @Test
    void rejectsAnOperationThatTheCurrentStateDoesNotAllow() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));

        assertThrows(RequestStateTransitionNotAllowed.class, () -> requestService.publish(uuid, 0L));
        assertEquals(RequestStatus.CREATED, storedRequest(uuid).getStatus());
    }

    @Test
    void aDeletedRequestIsNoLongerReachable() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));

        requestService.delete(new RequestUpdateStatusWithReason(uuid, "no longer needed"), 0L);
        assertEquals(RequestStatus.DELETED, storedRequest(uuid).getStatus());

        assertThrows(RequestNotFoundException.class, () -> requestService.getRequestById(uuid));
        assertThrows(RequestNotFoundException.class, () -> requestService.delete(
                new RequestUpdateStatusWithReason(uuid, "no longer needed"), 1L));
    }

    @Test
    void getRequestByIdThrowsWhenTheRequestDoesNotExist() {
        assertThrows(RequestNotFoundException.class, () -> requestService.getRequestById("does-not-exist"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void everyOperationRefusesAnUnusableRequestId(String requestId) {
        assertRefuses("requestId", () -> requestService.getRequestById(requestId));
        assertRefuses("requestId", () -> requestService.verify(requestId, 0L));
        assertRefuses("requestId", () -> requestService.accept(requestId, 0L));
        assertRefuses("requestId", () -> requestService.publish(requestId, 0L));
        assertRefuses("requestId", () -> requestService.reject(new RequestUpdateStatusWithReason(requestId, "a reason"), 0L));
        assertRefuses("requestId", () -> requestService.delete(new RequestUpdateStatusWithReason(requestId, "a reason"), 0L));
        assertRefuses("requestId", () -> requestService.updateRequestBody(
                new RequestUpdateBodyCommand(requestId, "a body"), 0L));
    }

    @Test
    void refusesACommandThatIsNotThere() {
        assertRefuses("command", () -> requestService.createRequest(null));
        assertRefuses("command", () -> requestService.updateRequestBody(null, 0L));
        assertRefuses("command", () -> requestService.reject(null, 0L));
        assertRefuses("command", () -> requestService.delete(null, 0L));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void anEmptyBodyIsRefusedByTheAggregate(String body) {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));

        assertThrows(MandatoryDataMissingException.class, () -> requestService.updateRequestBody(
                new RequestUpdateBodyCommand(uuid, body), 0L));
        assertEquals("the body", storedRequest(uuid).getBody());
    }

    @Test
    void theBodyCanNoLongerBeUpdatedOnceTheRequestIsAccepted() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));
        requestService.verify(uuid, 0L);
        requestService.accept(uuid, 1L);

        assertThrows(OperationNotAllowedInCurrentState.class, () -> requestService.updateRequestBody(
                new RequestUpdateBodyCommand(uuid, "a new body"), 2L));
        assertEquals("the body", storedRequest(uuid).getBody());
    }

    @Test
    void everyWriteMovesTheRequestOnToTheNextVersion() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));
        assertEquals(Request.INITIAL_VERSION, requestService.getRequestById(uuid).getVersion());

        requestService.verify(uuid, 0L);
        assertEquals(1L, requestService.getRequestById(uuid).getVersion());

        requestService.updateRequestBody(new RequestUpdateBodyCommand(uuid, "a new body"), 1L);
        assertEquals(2L, requestService.getRequestById(uuid).getVersion());

        requestService.accept(uuid, 2L);
        assertEquals(3L, requestService.getRequestById(uuid).getVersion());
    }

    @Test
    void everyOperationRefusesAVersionTheRequestHasMovedOnFrom() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));
        final long staleVersion = requestService.getRequestById(uuid).getVersion();
        requestService.verify(uuid, staleVersion);

        assertRefusesStaleVersion(() -> requestService.accept(uuid, staleVersion));
        assertRefusesStaleVersion(() -> requestService.publish(uuid, staleVersion));
        assertRefusesStaleVersion(() -> requestService.reject(new RequestUpdateStatusWithReason(uuid, "a reason"), staleVersion));
        assertRefusesStaleVersion(() -> requestService.delete(new RequestUpdateStatusWithReason(uuid, "a reason"), staleVersion));
        assertRefusesStaleVersion(() -> requestService.updateRequestBody(
                new RequestUpdateBodyCommand(uuid, "a new body"), staleVersion));

        assertEquals(RequestStatus.VERIFIED, storedRequest(uuid).getStatus());
        assertEquals("the body", storedRequest(uuid).getBody());
    }

    @Test
    void refusesTheWriteWhenAnotherWriterGotThereFirst() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));
        requestRepo.letAnotherWriterWinTheNextRead();

        assertThrows(StaleRequestVersionException.class, () -> requestService.verify(uuid, 0L));
        assertEquals(RequestStatus.CREATED, storedRequest(uuid).getStatus());
    }

    @Test
    void everyWriteIsRecordedInTheHistory() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));
        requestService.updateRequestBody(new RequestUpdateBodyCommand(uuid, "a rewritten body"), 0L);
        requestService.verify(uuid, 1L);
        requestService.accept(uuid, 2L);
        requestService.publish(uuid, 3L);

        assertEquals(List.of(RequestEventType.CREATED, RequestEventType.BODY_UPDATED, RequestEventType.VERIFIED,
                        RequestEventType.ACCEPTED, RequestEventType.PUBLISHED),
                outbox.typesRecordedFor(uuid));
    }

    @Test
    void aRefusedChangeIsNotRecorded() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));
        outbox.forgetEverything();

        assertThrows(RequestStateTransitionNotAllowed.class, () -> requestService.publish(uuid, 0L));
        assertThrows(MandatoryDataMissingException.class, () -> requestService.delete(
                new RequestUpdateStatusWithReason(uuid, null), 0L));

        assertTrue(outbox.typesRecordedFor(uuid).isEmpty());
    }

    @Test
    void aWriteTheStoreRefusesIsNotRecorded() {
        final String uuid = requestService.createRequest(new NewRequestCommand("first request", "the body"));
        requestRepo.letAnotherWriterWinTheNextRead();
        outbox.forgetEverything();

        assertThrows(StaleRequestVersionException.class, () -> requestService.verify(uuid, 0L));

        assertTrue(outbox.typesRecordedFor(uuid).isEmpty());
    }

    private void assertRefuses(String expectedFieldName, Executable operation) {
        assertEquals(expectedFieldName, assertThrows(InvalidCommandException.class, operation).getFieldName());
    }

    private void assertRefusesStaleVersion(Executable operation) {
        assertThrows(StaleRequestVersionException.class, operation);
    }

    private Request storedRequest(String uuid) {
        return requestRepo.findRequestById(uuid, RequestLookupScope.ALL);
    }

    private static class InMemoryRequestRepo implements RequestRepo {

        private final Map<String, Request> requests = new HashMap<>();

        private boolean anotherWriterWinsTheNextRead;

        @Override
        public Request findRequestById(String requestId, RequestLookupScope scope) {
            final Request request = requests.get(requestId);
            if (request == null
                    || (scope == RequestLookupScope.EXCLUDE_DELETED && request.getStatus() == RequestStatus.DELETED)) {
                return null;
            }

            final Request readState = snapshotOf(request);
            if (anotherWriterWinsTheNextRead) {
                anotherWriterWinsTheNextRead = false;
                request.writtenAsVersion(request.getVersion() + 1);
            }
            return readState;
        }

        @Override
        public void save(Request request) {
            requests.put(request.getUuid(), snapshotOf(request));
        }

        @Override
        public void update(Request request) {
            final Request stored = requests.get(request.getUuid());
            if (stored == null || !stored.isAtVersion(request.getVersion())) {
                throw new StaleRequestVersionException(request.getUuid(), request.getVersion());
            }

            final Request written = snapshotOf(request);
            written.writtenAsVersion(request.getVersion() + 1);
            requests.put(request.getUuid(), written);
            request.writtenAsVersion(request.getVersion() + 1);
        }

        void letAnotherWriterWinTheNextRead() {
            this.anotherWriterWinsTheNextRead = true;
        }

        private static Request snapshotOf(Request request) {
            return Request.reconstitute(request.getUuid(), request.getName(), request.getBody(), request.getStatus(),
                    request.getPublishedRequestUuid(), request.getCreatedAt(), request.getDisabledDate(),
                    request.getReason(), request.getVersion());
        }
    }

    private static class InMemoryRequestEventOutbox implements RequestEventOutbox {

        private final List<RequestEvent> appended = new ArrayList<>();

        @Override
        public void append(List<RequestEvent> events) {
            this.appended.addAll(events);
        }

        List<RequestEventType> typesRecordedFor(String requestUuid) {
            return this.appended.stream()
                    .filter(event -> event.requestUuid().equals(requestUuid))
                    .map(RequestEvent::type)
                    .toList();
        }

        void forgetEverything() {
            this.appended.clear();
        }
    }
}
