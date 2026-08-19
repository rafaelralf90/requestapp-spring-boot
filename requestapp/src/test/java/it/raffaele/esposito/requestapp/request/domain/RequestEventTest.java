package it.raffaele.esposito.requestapp.request.domain;

import it.raffaele.esposito.requestapp.request.domain.event.RequestEvent;
import it.raffaele.esposito.requestapp.request.domain.event.RequestEventType;
import it.raffaele.esposito.requestapp.request.domain.exceptions.MandatoryDataMissingException;
import it.raffaele.esposito.requestapp.request.domain.exceptions.OperationNotAllowedInCurrentState;
import it.raffaele.esposito.requestapp.request.domain.exceptions.RequestStateTransitionNotAllowed;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestEventTest {

    @Test
    void beingCreatedIsTheFirstThingARequestRecords() {
        final Request request = new Request("first request", "the body");

        final RequestEvent created = onlyEventOf(request);
        assertEquals(RequestEventType.CREATED, created.type());
        assertEquals(request.getUuid(), created.requestUuid());
        assertNull(created.fromStatus(), "a request does not come from a status into being");
        assertEquals(RequestStatus.CREATED, created.toStatus());
        assertEquals(Request.INITIAL_VERSION, created.decidedOnVersion());
        assertNotNull(created.occurredAt());
    }

    @Test
    void everyTransitionRecordsWhereItCameFromAndWhereItWent() {
        final Request request = new Request("first request", "the body");
        request.pullEvents();

        request.verify();
        assertTransition(onlyEventOf(request), RequestEventType.VERIFIED,
                RequestStatus.CREATED, RequestStatus.VERIFIED);

        request.accept();
        assertTransition(onlyEventOf(request), RequestEventType.ACCEPTED,
                RequestStatus.VERIFIED, RequestStatus.ACCEPTED);
    }

    @Test
    void rewritingTheBodyIsRecordedWithoutMovingTheStatus() {
        final Request request = new Request("first request", "the body");
        request.pullEvents();

        request.updateBody("a rewritten body");

        assertTransition(onlyEventOf(request), RequestEventType.BODY_UPDATED,
                RequestStatus.CREATED, RequestStatus.CREATED);
    }

    @Test
    void publishingRecordsTheIdentityItMinted() {
        final Request request = new Request("first request", "the body");
        request.verify();
        request.accept();
        request.pullEvents();

        final String publishedRequestUuid = request.publish();

        final RequestEvent published = onlyEventOf(request);
        assertEquals(RequestEventType.PUBLISHED, published.type());
        assertEquals(publishedRequestUuid, published.publishedRequestUuid());
    }

    @Test
    void closingARequestRecordsTheReasonItWasClosedFor() {
        final Request rejected = new Request("first request", "the body");
        rejected.verify();
        rejected.pullEvents();
        rejected.reject("does not meet the guidelines");
        assertEquals("does not meet the guidelines", onlyEventOf(rejected).reason());

        final Request deleted = new Request("first request", "the body");
        deleted.pullEvents();
        deleted.delete("no longer needed");
        assertEquals("no longer needed", onlyEventOf(deleted).reason());
    }

    @Test
    void aRefusedChangeRecordsNothing() {
        final Request request = new Request("first request", "the body");
        request.pullEvents();

        assertThrows(RequestStateTransitionNotAllowed.class, request::publish);
        assertThrows(MandatoryDataMissingException.class, () -> request.delete(null));
        assertThrows(MandatoryDataMissingException.class, () -> request.updateBody(" "));

        assertTrue(request.pullEvents().isEmpty());
    }

    @Test
    void aBodyRewriteRefusedByTheStatusRecordsNothing() {
        final Request request = new Request("first request", "the body");
        request.verify();
        request.accept();
        request.pullEvents();

        assertThrows(OperationNotAllowedInCurrentState.class, () -> request.updateBody("a rewritten body"));

        assertTrue(request.pullEvents().isEmpty());
    }

    @Test
    void aRequestHandsOverItsWholeHistoryInTheOrderItHappened() {
        final Request request = new Request("first request", "the body");
        request.updateBody("a rewritten body");
        request.verify();
        request.accept();
        request.publish();

        assertEquals(List.of(RequestEventType.CREATED, RequestEventType.BODY_UPDATED, RequestEventType.VERIFIED,
                        RequestEventType.ACCEPTED, RequestEventType.PUBLISHED),
                request.pullEvents().stream().map(RequestEvent::type).toList());
    }

    @Test
    void handingTheEventsOverForgetsThem() {
        final Request request = new Request("first request", "the body");

        assertEquals(1, request.pullEvents().size());
        assertTrue(request.pullEvents().isEmpty());
    }

    @Test
    void aRequestRebuiltFromTheStoreHasNothingToRecord() {
        final Request request = Request.reconstitute("an-uuid", "first request", "the body",
                RequestStatus.VERIFIED, null, java.time.Instant.now(), null, null, 3L);

        assertTrue(request.pullEvents().isEmpty());
    }

    @Test
    void everyEventRecordsTheVersionItWasDecidedOn() {
        final Request request = Request.reconstitute("an-uuid", "first request", "the body",
                RequestStatus.CREATED, null, java.time.Instant.now(), null, null, 7L);

        request.verify();

        assertEquals(7L, onlyEventOf(request).decidedOnVersion());
    }

    private static void assertTransition(RequestEvent event, RequestEventType type,
                                         RequestStatus from, RequestStatus to) {
        assertEquals(type, event.type());
        assertEquals(from, event.fromStatus());
        assertEquals(to, event.toStatus());
    }

    private static RequestEvent onlyEventOf(Request request) {
        final List<RequestEvent> events = request.pullEvents();
        assertEquals(1, events.size(), "a change records exactly one event");
        return events.get(0);
    }
}
