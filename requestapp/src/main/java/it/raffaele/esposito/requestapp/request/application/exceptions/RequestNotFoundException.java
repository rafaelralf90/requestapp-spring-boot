package it.raffaele.esposito.requestapp.request.application.exceptions;

import it.raffaele.esposito.requestapp.request.domain.exceptions.RequestException;

public class RequestNotFoundException extends RequestException {

    public RequestNotFoundException(String message){
        super(message);
    }
}
