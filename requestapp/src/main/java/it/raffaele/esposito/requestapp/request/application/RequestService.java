package it.raffaele.esposito.requestapp.request.application;

import it.raffaele.esposito.requestapp.request.application.exceptions.InvalidCommandException;
import it.raffaele.esposito.requestapp.request.application.exceptions.RequestNotFoundException;
import it.raffaele.esposito.requestapp.request.domain.Request;
import it.raffaele.esposito.requestapp.request.ports.in.RequestServicePort;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.NewRequestCommand;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.RequestUpdateStatusWithReason;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.RequestUpdateBodyCommand;
import it.raffaele.esposito.requestapp.request.ports.in.dto.out.RequestData;
import it.raffaele.esposito.requestapp.request.ports.out.outbox.RequestEventOutbox;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestLookupScope;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestRepo;
import it.raffaele.esposito.requestapp.request.application.exceptions.StaleRequestVersionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@RequiredArgsConstructor
@Service
public class RequestService implements RequestServicePort {

    private final RequestRepo requestRepo;

    private final RequestEventOutbox requestEventOutbox;

    private final Clock clock;

    @Transactional(readOnly = true)
    @Override
    public RequestData getRequestById(String requestId) {

        return RequestData.from(getRequest(requestId));
    }

    @Transactional
    @Override
    public String createRequest(NewRequestCommand newRequestCommand) {

        requireCommand(newRequestCommand);

        final Request request = new Request(newRequestCommand.getName(), newRequestCommand.getBody(), this.clock);
        this.requestRepo.save(request);
        recordWhatHappened(request);
        return request.getUuid();
    }

    @Transactional
    @Override
    public void updateRequestBody(RequestUpdateBodyCommand requestUpdateBodyCommand, long version) {

        requireCommand(requestUpdateBodyCommand);

        final Request request = getRequest(requestUpdateBodyCommand.getUuid(), version);
        request.updateBody(requestUpdateBodyCommand.getBody());
        store(request);
    }

    @Transactional
    @Override
    public void verify(String requestId, long version) {

        final Request request = getRequest(requestId, version);
        request.verify();
        store(request);
    }

    @Transactional
    @Override
    public void accept(String requestId, long version) {

        final Request request = getRequest(requestId, version);
        request.accept();
        store(request);
    }

    @Transactional
    @Override
    public void reject(RequestUpdateStatusWithReason requestUpdateStatusWithReason, long version) {

        requireCommand(requestUpdateStatusWithReason);

        final Request request = getRequest(requestUpdateStatusWithReason.getUuid(), version);
        request.reject(requestUpdateStatusWithReason.getReason());
        store(request);
    }

    @Transactional
    @Override
    public String publish(String requestId, long version) {

        final Request request = getRequest(requestId, version);
        final String publishedRequestUuid = request.publish();
        store(request);
        return publishedRequestUuid;
    }

    @Transactional
    @Override
    public void delete(RequestUpdateStatusWithReason requestUpdateStatusWithReason, long version) {

        requireCommand(requestUpdateStatusWithReason);

        final Request request = getRequest(requestUpdateStatusWithReason.getUuid(), version);
        request.delete(requestUpdateStatusWithReason.getReason());
        store(request);
    }

    private void store(Request request) {

        this.requestRepo.update(request);
        recordWhatHappened(request);
    }

    private void recordWhatHappened(Request request) {

        this.requestEventOutbox.append(request.pullEvents());
    }

    private Request getRequest(String requestId, long version) {

        final Request existingRequest = getRequest(requestId);
        if (!existingRequest.isAtVersion(version)) {
            throw new StaleRequestVersionException(requestId, version);
        }
        return existingRequest;
    }

    private Request getRequest(String requestId) {

        if (requestId == null || requestId.isBlank()) {
            throw new InvalidCommandException("requestId");
        }

        final Request existingRequest = requestRepo.findRequestById(requestId, RequestLookupScope.EXCLUDE_DELETED);
        if (existingRequest == null) {
            throw new RequestNotFoundException("Request not found, id: " + requestId);
        }
        return existingRequest;
    }

    private static void requireCommand(Object command) {

        if (command == null) {
            throw new InvalidCommandException("command");
        }
    }
}
