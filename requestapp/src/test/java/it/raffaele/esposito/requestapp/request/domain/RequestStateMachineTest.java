package it.raffaele.esposito.requestapp.request.domain;

import it.raffaele.esposito.requestapp.request.domain.exceptions.OperationNotAllowedInCurrentState;
import it.raffaele.esposito.requestapp.request.domain.exceptions.RequestStateTransitionNotAllowed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestStateMachineTest {

    private static final Set<RequestStatus> VERIFY_ALLOWED_FROM = EnumSet.of(RequestStatus.CREATED);
    private static final Set<RequestStatus> ACCEPT_ALLOWED_FROM = EnumSet.of(RequestStatus.VERIFIED);
    private static final Set<RequestStatus> REJECT_ALLOWED_FROM = EnumSet.of(RequestStatus.VERIFIED, RequestStatus.ACCEPTED);
    private static final Set<RequestStatus> PUBLISH_ALLOWED_FROM = EnumSet.of(RequestStatus.ACCEPTED);
    private static final Set<RequestStatus> DELETE_ALLOWED_FROM = EnumSet.of(RequestStatus.CREATED);
    private static final Set<RequestStatus> UPDATE_BODY_ALLOWED_FROM =
            EnumSet.of(RequestStatus.CREATED, RequestStatus.VERIFIED);

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void verifyIsOnlyAllowedFromCreated(RequestStatus from) {
        assertTransition(from, VERIFY_ALLOWED_FROM, RequestStatus.VERIFIED, Request::verify);
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void acceptIsOnlyAllowedFromVerified(RequestStatus from) {
        assertTransition(from, ACCEPT_ALLOWED_FROM, RequestStatus.ACCEPTED, Request::accept);
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void rejectIsOnlyAllowedFromVerifiedAndAccepted(RequestStatus from) {
        assertTransition(from, REJECT_ALLOWED_FROM, RequestStatus.REJECTED, r -> r.reject("a reason"));
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void publishIsOnlyAllowedFromAccepted(RequestStatus from) {
        assertTransition(from, PUBLISH_ALLOWED_FROM, RequestStatus.PUBLISHED, Request::publish);
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void deleteIsOnlyAllowedFromCreated(RequestStatus from) {
        assertTransition(from, DELETE_ALLOWED_FROM, RequestStatus.DELETED, r -> r.delete("a reason"));
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void theBodyCanOnlyBeUpdatedWhileCreatedOrVerified(RequestStatus from) {
        final Request request = requestInStatus(from);

        if (UPDATE_BODY_ALLOWED_FROM.contains(from)) {
            request.updateBody("a new body");
            assertEquals("a new body", request.getBody());
        } else {
            assertThrows(OperationNotAllowedInCurrentState.class, () -> request.updateBody("a new body"));
            assertEquals("the original body", request.getBody());
        }
    }

    @Test
    void publishingMintsThePublishedRequestUuid() {
        final Request request = requestInStatus(RequestStatus.ACCEPTED);
        assertNull(request.getPublishedRequestUuid());

        final String publishedRequestUuid = request.publish();

        assertNotNull(publishedRequestUuid);
        assertEquals(publishedRequestUuid, request.getPublishedRequestUuid());
    }

    @Test
    void deletingStampsTheDisabledDate() {
        final Request request = requestInStatus(RequestStatus.CREATED);
        assertNull(request.getDisabledDate());

        request.delete("no longer needed");

        assertNotNull(request.getDisabledDate());
    }

    @Test
    void aTerminalRequestAcceptsNoFurtherTransition() {
        for (RequestStatus terminal : EnumSet.of(RequestStatus.DELETED, RequestStatus.REJECTED, RequestStatus.PUBLISHED)) {
            final Request request = requestInStatus(terminal);
            assertThrows(RequestStateTransitionNotAllowed.class, request::verify);
            assertThrows(RequestStateTransitionNotAllowed.class, request::accept);
            assertThrows(RequestStateTransitionNotAllowed.class, () -> request.reject("a reason"));
            assertThrows(RequestStateTransitionNotAllowed.class, request::publish);
            assertThrows(RequestStateTransitionNotAllowed.class, () -> request.delete("a reason"));
        }
    }

    @Test
    void aRequestStartsAsCreated() {
        assertEquals(RequestStatus.CREATED, new Request("a request", "the body").getStatus());
    }

    private void assertTransition(RequestStatus from, Set<RequestStatus> allowedFrom, RequestStatus expected,
                                  Consumer<Request> operation) {
        final Request request = requestInStatus(from);

        if (allowedFrom.contains(from)) {
            operation.accept(request);
            assertEquals(expected, request.getStatus());
        } else {
            assertThrows(RequestStateTransitionNotAllowed.class, () -> operation.accept(request));
            assertEquals(from, request.getStatus(), "a rejected transition must leave the status untouched");
        }
    }

    private Request requestInStatus(RequestStatus status) {
        return Request.reconstitute(UUID.randomUUID().toString(), "a request", "the original body",
                status, null, Instant.now(), null, null, Request.INITIAL_VERSION);
    }
}
