package it.raffaele.esposito.requestapp.request.domain;

import it.raffaele.esposito.requestapp.request.domain.exceptions.FieldTooLongException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestFieldLengthTest {

    @Test
    void aNameAtTheLimitIsAccepted() {
        final Request request = new Request(of(Request.NAME_MAX_LENGTH), "the body");

        assertEquals(Request.NAME_MAX_LENGTH, request.getName().length());
    }

    @Test
    void aNameOverTheLimitIsRefused() {
        final FieldTooLongException e = assertThrows(FieldTooLongException.class,
                () -> new Request(of(Request.NAME_MAX_LENGTH + 1), "the body"));

        assertEquals("name", e.getFieldName());
        assertEquals(Request.NAME_MAX_LENGTH, e.getMaxLength());
    }

    @Test
    void aBodyAtTheLimitIsAccepted() {
        final Request request = new Request("a request", of(Request.BODY_MAX_LENGTH));

        assertEquals(Request.BODY_MAX_LENGTH, request.getBody().length());
    }

    @Test
    void aBodyOverTheLimitIsRefused() {
        final FieldTooLongException e = assertThrows(FieldTooLongException.class,
                () -> new Request("a request", of(Request.BODY_MAX_LENGTH + 1)));

        assertEquals("body", e.getFieldName());
        assertEquals(Request.BODY_MAX_LENGTH, e.getMaxLength());
    }

    @Test
    void theBodyCannotBeUpdatedToAnOversizedValue() {
        final Request request = new Request("a request", "the original body");

        final FieldTooLongException e = assertThrows(FieldTooLongException.class,
                () -> request.updateBody(of(Request.BODY_MAX_LENGTH + 1)));

        assertEquals("body", e.getFieldName());
        assertEquals("the original body", request.getBody(), "a refused update must not change the body");
    }

    @Test
    void aRequestCannotBeRejectedWithAnOversizedReason() {
        final Request request = new Request("a request", "the body");
        request.verify();

        final FieldTooLongException e = assertThrows(FieldTooLongException.class,
                () -> request.reject(of(Request.REASON_MAX_LENGTH + 1)));

        assertEquals("reason", e.getFieldName());
        assertEquals(RequestStatus.VERIFIED, request.getStatus(), "a refused rejection must not change the status");
    }

    @Test
    void aRequestCannotBeDeletedWithAnOversizedReason() {
        final Request request = new Request("a request", "the body");

        final FieldTooLongException e = assertThrows(FieldTooLongException.class,
                () -> request.delete(of(Request.REASON_MAX_LENGTH + 1)));

        assertEquals("reason", e.getFieldName());
        assertEquals(RequestStatus.CREATED, request.getStatus(), "a refused deletion must not change the status");
    }

    @Test
    void aLengthRuleIsNotAppliedToAStoredRequestBeingReadBack() {
        final Request stored = Request.reconstitute("an-id", of(Request.NAME_MAX_LENGTH), of(Request.BODY_MAX_LENGTH),
                RequestStatus.CREATED, null, java.time.Instant.now(), null, null, Request.INITIAL_VERSION);

        assertEquals(Request.NAME_MAX_LENGTH, stored.getName().length());
    }

    private static String of(int length) {
        return "x".repeat(length);
    }
}
