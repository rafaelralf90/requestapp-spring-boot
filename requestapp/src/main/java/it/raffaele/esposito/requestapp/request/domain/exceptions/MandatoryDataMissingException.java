package it.raffaele.esposito.requestapp.request.domain.exceptions;

public class MandatoryDataMissingException extends RequestException {

    private final String fieldName;

    public MandatoryDataMissingException(String fieldName){
        super("A request requires a non empty " + fieldName + ".");
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
