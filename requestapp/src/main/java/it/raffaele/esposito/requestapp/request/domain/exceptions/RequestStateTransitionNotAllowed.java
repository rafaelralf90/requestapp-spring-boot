package it.raffaele.esposito.requestapp.request.domain.exceptions;

public class RequestStateTransitionNotAllowed extends OperationNotAllowedInCurrentState {

    public RequestStateTransitionNotAllowed(String message){
        super(message);
    }
}
