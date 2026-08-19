package it.raffaele.esposito.requestapp.request.domain.exceptions;

public class FieldTooLongException extends RequestException {

    private final String fieldName;

    private final int maxLength;

    public FieldTooLongException(String fieldName, int maxLength) {
        super("A request " + fieldName + " cannot be longer than " + maxLength + " characters.");
        this.fieldName = fieldName;
        this.maxLength = maxLength;
    }

    public String getFieldName() {
        return fieldName;
    }

    public int getMaxLength() {
        return maxLength;
    }
}
