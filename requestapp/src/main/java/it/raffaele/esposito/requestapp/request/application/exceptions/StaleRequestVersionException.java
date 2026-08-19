package it.raffaele.esposito.requestapp.request.application.exceptions;

import it.raffaele.esposito.requestapp.request.domain.exceptions.RequestException;

public class StaleRequestVersionException extends RequestException {

    private final long expectedVersion;

    public StaleRequestVersionException(String requestId, long expectedVersion) {
        super("Request " + requestId + " has been modified since version " + expectedVersion
                + ", read it again before writing.");
        this.expectedVersion = expectedVersion;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }
}
