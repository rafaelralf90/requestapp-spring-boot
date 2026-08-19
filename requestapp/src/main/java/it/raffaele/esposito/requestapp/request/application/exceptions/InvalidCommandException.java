package it.raffaele.esposito.requestapp.request.application.exceptions;

import it.raffaele.esposito.requestapp.request.domain.exceptions.RequestException;

public class InvalidCommandException extends RequestException {

    private final String fieldName;

    public InvalidCommandException(String fieldName) {
        super("A non empty " + fieldName + " is required.");
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
