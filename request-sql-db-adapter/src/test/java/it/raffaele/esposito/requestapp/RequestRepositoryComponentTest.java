package it.raffaele.esposito.requestapp;

import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity.RequestEntity;
import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.repo.mybatis.RequestMapperRepo;
import it.raffaele.esposito.requestapp.request.domain.Request;
import it.raffaele.esposito.requestapp.request.domain.RequestStatus;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestLookupScope;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestRepo;
import it.raffaele.esposito.requestapp.request.application.exceptions.StaleRequestVersionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TestApplication.class)
@ExtendWith(SpringExtension.class)
public class RequestRepositoryComponentTest {

    @Autowired
    private RequestRepo requestRepo;

    @Autowired
    private RequestMapperRepo requestMapperRepo;

    @Test
    void savesARequestAndReadsItBack() {
        final Request request = new Request("first request", "the body of the request");

        requestRepo.save(request);

        final Request found = requestRepo.findRequestById(request.getUuid(), RequestLookupScope.EXCLUDE_DELETED);
        assertThat(found).isNotNull();
        assertThat(found.getUuid()).isEqualTo(request.getUuid());
        assertThat(found.getName()).isEqualTo("first request");
        assertThat(found.getBody()).isEqualTo("the body of the request");
        assertThat(found.getStatus()).isEqualTo(RequestStatus.CREATED);
        assertThat(found.getCreatedAt())
                .isBetween(request.getCreatedAt().minusSeconds(1), request.getCreatedAt().plusSeconds(1));
    }

    @Test
    void returnsNullForAnUnknownId() {
        assertThat(requestRepo.findRequestById("does-not-exist", RequestLookupScope.EXCLUDE_DELETED)).isNull();
    }

    @Test
    void updatesEveryColumnExceptTheIdentity() {
        final Request request = new Request("original name", "original body");
        requestRepo.save(request);

        final Request modified = Request.reconstitute(request.getUuid(), "updated name", "updated body",
                RequestStatus.CREATED, null, request.getCreatedAt(), null, null, request.getVersion());
        requestRepo.update(modified);

        final Request found = requestRepo.findRequestById(request.getUuid(), RequestLookupScope.EXCLUDE_DELETED);
        assertThat(found).isNotNull();
        assertThat(found.getUuid()).isEqualTo(request.getUuid());
        assertThat(found.getName()).isEqualTo("updated name");
        assertThat(found.getBody()).isEqualTo("updated body");
    }

    @Test
    void persistsThePublishedRequestUuidWhenARequestIsPublished() {
        final Request request = new Request("a request to publish", "the body of the request");
        requestRepo.save(request);

        request.verify();
        request.accept();
        final String publishedRequestUuid = request.publish();
        requestRepo.update(request);

        final Request found = requestRepo.findRequestById(request.getUuid(), RequestLookupScope.EXCLUDE_DELETED);
        assertThat(found).isNotNull();
        assertThat(found.getStatus()).isEqualTo(RequestStatus.PUBLISHED);
        assertThat(found.getPublishedRequestUuid()).isEqualTo(publishedRequestUuid);
    }

    @Test
    void hidesADeletedRequestUnlessTheLookupAsksForEverything() {
        final Request deletedRequest = Request.reconstitute(UUID.randomUUID().toString(), "a deleted request",
                "the body of the request", RequestStatus.DELETED, null, Instant.now(), Instant.now(),
                "no longer needed", Request.INITIAL_VERSION);
        requestRepo.save(deletedRequest);

        assertThat(requestRepo.findRequestById(deletedRequest.getUuid(), RequestLookupScope.EXCLUDE_DELETED)).isNull();

        final Request found = requestRepo.findRequestById(deletedRequest.getUuid(), RequestLookupScope.ALL);
        assertThat(found).isNotNull();
        assertThat(found.getStatus()).isEqualTo(RequestStatus.DELETED);
        assertThat(found.getDisabledDate()).isNotNull();
    }

    @Test
    void persistsTheDisabledDateWhenARequestIsDeleted() {
        final Request request = new Request("a request to delete", "the body of the request");
        requestRepo.save(request);

        request.delete("no longer needed");
        requestRepo.update(request);

        final RequestEntity entity = requestMapperRepo.findRequestById(request.getUuid(), false);
        assertThat(entity).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(RequestStatus.DELETED.name());
        assertThat(entity.getDisabledDate()).isNotNull();
        assertThat(entity.getReason()).isEqualTo("no longer needed");
    }

    @Test
    void aSavedRequestStartsAtTheInitialVersionAndEveryWriteMovesTheStoredOneOnByOne() {
        final Request request = new Request("a request", "the body of the request");
        requestRepo.save(request);

        assertThat(loaded(request).getVersion()).isEqualTo(Request.INITIAL_VERSION);

        final Request firstWriter = loaded(request);
        firstWriter.verify();
        requestRepo.update(firstWriter);

        assertThat(loaded(request).getVersion()).isEqualTo(Request.INITIAL_VERSION + 1);
        assertThat(firstWriter.getVersion())
                .describedAs("the aggregate that was written must know the version it landed at")
                .isEqualTo(Request.INITIAL_VERSION + 1);

        final Request secondWriter = loaded(request);
        secondWriter.accept();
        requestRepo.update(secondWriter);

        assertThat(loaded(request).getVersion()).isEqualTo(Request.INITIAL_VERSION + 2);
    }

    @Test
    void refusesAWriteAgainstAVersionTheStoredRequestHasMovedOnFrom() {
        final Request request = new Request("a contested request", "the body of the request");
        requestRepo.save(request);

        final Request readByFirstCaller = loaded(request);
        final Request readBySecondCaller = loaded(request);

        readByFirstCaller.verify();
        requestRepo.update(readByFirstCaller);

        readBySecondCaller.delete("no longer needed");
        assertThatThrownBy(() -> requestRepo.update(readBySecondCaller))
                .isInstanceOf(StaleRequestVersionException.class);

        final Request stored = loaded(request);
        assertThat(stored.getStatus()).isEqualTo(RequestStatus.VERIFIED);
        assertThat(stored.getVersion()).isEqualTo(Request.INITIAL_VERSION + 1);
    }

    @Test
    void aRefusedWriteDoesNotMoveTheRequestItRefused() {
        final Request request = new Request("a contested request", "the body of the request");
        requestRepo.save(request);

        final Request readBySecondCaller = loaded(request);
        requestRepo.update(loaded(request));

        assertThatThrownBy(() -> requestRepo.update(readBySecondCaller))
                .isInstanceOf(StaleRequestVersionException.class);
        assertThat(readBySecondCaller.getVersion())
                .describedAs("a refused write must not move the version of the aggregate it refused")
                .isEqualTo(Request.INITIAL_VERSION);
    }

    @Test
    void persistsTheReasonWhenARequestIsRejected() {
        final Request request = new Request("a request to reject", "the body of the request");
        requestRepo.save(request);

        request.verify();
        request.reject("the content does not meet the guidelines");
        requestRepo.update(request);

        final Request found = requestRepo.findRequestById(request.getUuid(), RequestLookupScope.EXCLUDE_DELETED);
        assertThat(found).isNotNull();
        assertThat(found.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(found.getReason()).isEqualTo("the content does not meet the guidelines");
    }

    private Request loaded(Request request) {
        return requestRepo.findRequestById(request.getUuid(), RequestLookupScope.ALL);
    }
}
