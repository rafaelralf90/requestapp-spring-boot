package it.raffaele.esposito.requestapp.request.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestVersionTest {

    @Test
    void aNewRequestStartsAtTheInitialVersion() {
        assertEquals(Request.INITIAL_VERSION, new Request("a request", "the body").getVersion());
        assertTrue(new Request("a request", "the body").isAtVersion(Request.INITIAL_VERSION));
    }

    @Test
    void aRequestComesBackAtTheVersionItWasLoadedAt() {
        final Request request = requestAtVersion(7L);

        assertEquals(7L, request.getVersion());
        assertTrue(request.isAtVersion(7L));
        assertFalse(request.isAtVersion(6L));
    }

    @Test
    void aTransitionOnItsOwnLeavesTheVersionWhereItWas() {
        final Request request = requestAtVersion(7L);

        request.verify();
        request.accept();

        assertEquals(7L, request.getVersion());
    }

    @Test
    void aWrittenRequestMovesToTheVersionItWasWrittenAs() {
        final Request request = requestAtVersion(7L);

        request.writtenAsVersion(8L);

        assertEquals(8L, request.getVersion());
        assertTrue(request.isAtVersion(8L));
        assertFalse(request.isAtVersion(7L));
    }

    private Request requestAtVersion(long version) {
        return Request.reconstitute(UUID.randomUUID().toString(), "a request", "the original body",
                RequestStatus.CREATED, null, Instant.now(), null, null, version);
    }
}
