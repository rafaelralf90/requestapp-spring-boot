package it.raffaele.esposito.requestapp;

import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity.RequestEventEntityMapper;
import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.repo.mybatis.RequestEventMapperRepo;
import it.raffaele.esposito.requestapp.request.domain.Request;
import it.raffaele.esposito.requestapp.request.domain.RequestStatus;
import it.raffaele.esposito.requestapp.request.domain.event.RequestEvent;
import it.raffaele.esposito.requestapp.request.domain.event.RequestEventType;
import it.raffaele.esposito.requestapp.request.ports.out.outbox.RequestEventOutbox;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestLookupScope;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TestApplication.class)
@ExtendWith(SpringExtension.class)
public class RequestEventOutboxComponentTest {

    @Autowired
    private RequestEventOutbox requestEventOutbox;

    @Autowired
    private RequestEventMapperRepo requestEventMapperRepo;

    @Autowired
    private RequestRepo requestRepo;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Transactional
    @Test
    void appendsWhatARequestRaisedAndReadsItBack() {
        final Request request = new Request("first request", "the body of the request");
        request.verify();
        request.accept();
        final String publishedRequestUuid = request.publish();

        requestEventOutbox.append(request.pullEvents());

        final List<RequestEvent> history = historyOf(request.getUuid());
        assertThat(history).extracting(RequestEvent::type).containsExactly(RequestEventType.CREATED,
                RequestEventType.VERIFIED, RequestEventType.ACCEPTED, RequestEventType.PUBLISHED);

        final RequestEvent created = history.get(0);
        assertThat(created.fromStatus()).isNull();
        assertThat(created.toStatus()).isEqualTo(RequestStatus.CREATED);
        assertThat(created.decidedOnVersion()).isEqualTo(Request.INITIAL_VERSION);
        assertThat(created.occurredAt())
                .isBetween(request.getCreatedAt().minusSeconds(1), request.getCreatedAt().plusSeconds(1));

        final RequestEvent published = history.get(3);
        assertThat(published.fromStatus()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(published.toStatus()).isEqualTo(RequestStatus.PUBLISHED);
        assertThat(published.publishedRequestUuid()).isEqualTo(publishedRequestUuid);
    }

    @Transactional
    @Test
    void keepsTheReasonARequestWasClosedFor() {
        final Request request = new Request("first request", "the body of the request");
        request.delete("no longer needed");

        requestEventOutbox.append(request.pullEvents());

        assertThat(historyOf(request.getUuid()))
                .filteredOn(event -> event.type() == RequestEventType.DELETED)
                .singleElement()
                .satisfies(deleted -> assertThat(deleted.reason()).isEqualTo("no longer needed"));
    }

    @Transactional
    @Test
    void appendsRatherThanOverwrites() {
        final Request request = new Request("first request", "the body of the request");
        requestEventOutbox.append(request.pullEvents());
        request.verify();
        requestEventOutbox.append(request.pullEvents());

        assertThat(historyOf(request.getUuid())).hasSize(2);
    }

    @Transactional
    @Test
    void appendingNothingWritesNothing() {
        final Request request = new Request("first request", "the body of the request");
        request.pullEvents();

        requestEventOutbox.append(request.pullEvents());

        assertThat(historyOf(request.getUuid())).isEmpty();
    }

    @Test
    void refusesToAppendOutsideATransaction() {
        final Request request = new Request("first request", "the body of the request");

        assertThatThrownBy(() -> requestEventOutbox.append(request.pullEvents()))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);

        assertThat(historyOf(request.getUuid())).isEmpty();
    }

    @Transactional
    @Test
    void recordsAreNotMarkedAsPublishedByStoringThem() {
        final Request request = new Request("first request", "the body of the request");

        requestEventOutbox.append(request.pullEvents());

        assertThat(requestEventMapperRepo.findEventsByRequestId(request.getUuid()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getPublishedAt()).isNull();
                    assertThat(row.getId()).isNotNull();
                });
    }

    private List<RequestEvent> historyOf(String requestUuid) {
        return requestEventMapperRepo.findEventsByRequestId(requestUuid).stream()
                .map(RequestEventEntityMapper::toDomain)
                .toList();
    }

    @Test
    void aRollbackTakesTheHistoryWithTheRequest() {
        final Request request = new Request("first request", "the body of the request");

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            requestRepo.save(request);
            requestEventOutbox.append(request.pullEvents());
            throw new IllegalStateException("something goes wrong after both writes");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(requestRepo.findRequestById(request.getUuid(), RequestLookupScope.ALL)).isNull();
        assertThat(requestEventMapperRepo.findEventsByRequestId(request.getUuid())).isEmpty();
    }

    @Test
    void aCommitKeepsBothTheRequestAndItsHistory() {
        final Request request = new Request("first request", "the body of the request");

        transactionTemplate.executeWithoutResult(status -> {
            requestRepo.save(request);
            requestEventOutbox.append(request.pullEvents());
        });

        assertThat(requestRepo.findRequestById(request.getUuid(), RequestLookupScope.ALL)).isNotNull();
        assertThat(requestEventMapperRepo.findEventsByRequestId(request.getUuid())).hasSize(1);
    }
}
