package it.raffaele.esposito.requestapp.request.domain.exceptions;

public class OperationNotAllowedInCurrentState extends RequestException {

    public OperationNotAllowedInCurrentState(String message){
        super(message);
    }
}
