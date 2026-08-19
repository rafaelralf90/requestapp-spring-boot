package it.raffaele.esposito.requestapp.request.domain;

import it.raffaele.esposito.requestapp.request.domain.exceptions.MandatoryDataMissingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestMandatoryDataTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void aRequestCannotBeCreatedWithoutAName(String name) {
        final MandatoryDataMissingException e =
                assertThrows(MandatoryDataMissingException.class, () -> new Request(name, "the body"));
        assertEquals("name", e.getFieldName());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void aRequestCannotBeCreatedWithoutABody(String body) {
        final MandatoryDataMissingException e =
                assertThrows(MandatoryDataMissingException.class, () -> new Request("a request", body));
        assertEquals("body", e.getFieldName());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void theBodyCannotBeUpdatedToAnEmptyValue(String body) {
        final Request request = new Request("a request", "the original body");

        assertThrows(MandatoryDataMissingException.class, () -> request.updateBody(body));
        assertEquals("the original body", request.getBody());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void aRequestCannotBeRejectedWithoutAReason(String reason) {
        final Request request = verifiedRequest();

        final MandatoryDataMissingException e =
                assertThrows(MandatoryDataMissingException.class, () -> request.reject(reason));

        assertEquals("reason", e.getFieldName());
        assertEquals(RequestStatus.VERIFIED, request.getStatus(), "a refused rejection must not change the status");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void aRequestCannotBeDeletedWithoutAReason(String reason) {
        final Request request = new Request("a request", "the body");

        final MandatoryDataMissingException e =
                assertThrows(MandatoryDataMissingException.class, () -> request.delete(reason));

        assertEquals("reason", e.getFieldName());
        assertEquals(RequestStatus.CREATED, request.getStatus(), "a refused deletion must not change the status");
    }

    @Test
    void theRejectionReasonIsKept() {
        final Request request = verifiedRequest();

        request.reject("the content does not meet the guidelines");

        assertEquals(RequestStatus.REJECTED, request.getStatus());
        assertEquals("the content does not meet the guidelines", request.getReason());
    }

    @Test
    void theDeletionReasonIsKept() {
        final Request request = new Request("a request", "the body");

        request.delete("submitted by mistake");

        assertEquals(RequestStatus.DELETED, request.getStatus());
        assertEquals("submitted by mistake", request.getReason());
    }

    @Test
    void aLiveRequestHasNoReason() {
        assertNull(new Request("a request", "the body").getReason());
    }

    @Test
    void aRequestWithANameAndABodyIsCreated() {
        final Request request = new Request("a request", "the body");

        assertEquals("a request", request.getName());
        assertEquals("the body", request.getBody());
        assertEquals(RequestStatus.CREATED, request.getStatus());
    }

    private Request verifiedRequest() {
        final Request request = new Request("a request", "the body");
        request.verify();
        return request;
    }
}
