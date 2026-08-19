package it.raffaele.esposito.requestapp.request.domain;

import it.raffaele.esposito.requestapp.request.domain.event.RequestEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestTimeTest {

    private static final Instant NOW = Instant.parse("2026-08-19T10:15:30.123456Z");

    private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void aRequestIsCreatedAtTheTimeItsClockShows() {
        assertEquals(NOW, new Request("a request", "the body", FIXED).getCreatedAt());
    }

    @Test
    void anEventOccursAtTheTimeTheClockShows() {
        final Request request = new Request("a request", "the body", FIXED);

        final RequestEvent created = request.pullEvents().get(0);

        assertEquals(NOW, created.occurredAt());
    }

    @Test
    void aDeletionIsStampedWithTheTimeTheClockShows() {
        final Request request = new Request("a request", "the body", FIXED);

        request.delete("no longer needed");

        assertEquals(NOW, request.getDisabledDate());
    }

    @Test
    void everyEventOfOneOperationCarriesTheSameInstant() {
        final Request request = new Request("a request", "the body", FIXED);
        request.updateBody("a rewritten body");
        request.verify();

        assertEquals(1, request.pullEvents().stream().map(RequestEvent::occurredAt).distinct().count());
    }

    @Test
    void aReconstitutedRequestKeepsTimeUnderTheClockItIsGiven() {
        final Request stored = Request.reconstitute("an-uuid", "a request", "the body", RequestStatus.CREATED,
                null, NOW.minusSeconds(60), null, null, Request.INITIAL_VERSION, FIXED);

        stored.delete("no longer needed");

        assertEquals(NOW, stored.getDisabledDate());
    }
}
