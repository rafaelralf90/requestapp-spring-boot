package it.raffaele.esposito.requestapp.request.domain.exceptions;

public abstract class RequestException extends RuntimeException {

    protected RequestException(String message) {
        super(message);
    }
}
