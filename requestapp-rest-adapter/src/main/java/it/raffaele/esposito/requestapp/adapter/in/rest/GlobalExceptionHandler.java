package it.raffaele.esposito.requestapp.adapter.in.rest;

import it.raffaele.esposito.requestapp.adapter.in.rest.model.APIError;
import it.raffaele.esposito.requestapp.request.application.exceptions.InvalidCommandException;
import it.raffaele.esposito.requestapp.request.application.exceptions.RequestNotFoundException;
import it.raffaele.esposito.requestapp.request.domain.exceptions.FieldTooLongException;
import it.raffaele.esposito.requestapp.request.domain.exceptions.MandatoryDataMissingException;
import it.raffaele.esposito.requestapp.request.domain.exceptions.OperationNotAllowedInCurrentState;
import it.raffaele.esposito.requestapp.request.domain.exceptions.RequestException;
import it.raffaele.esposito.requestapp.request.domain.exceptions.RequestStateTransitionNotAllowed;
import it.raffaele.esposito.requestapp.request.application.exceptions.StaleRequestVersionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Collections;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final int REQUEST_NOT_FOUND_ERROR_CODE = 1;
    public static final int MALFORMED_EXCEPTION_ERROR_CODE = 2;
    public static final int STATE_TRANSITION_NOT_ALLOWED_ERROR_CODE = 3;
    public static final int OPERATION_NOT_ALLOWED_ERROR_CODE = 4;
    public static final int MANDATORY_FIELD_MISSING_ERROR_CODE = 5;
    public static final int INVALID_COMMAND_ERROR_CODE = 6;
    public static final int STALE_VERSION_ERROR_CODE = 7;
    public static final int UNSUPPORTED_MEDIA_TYPE_ERROR_CODE = 8;
    public static final int METHOD_NOT_ALLOWED_ERROR_CODE = 9;
    public static final int UNKNOWN_ENDPOINT_ERROR_CODE = 10;
    public static final int FIELD_TOO_LONG_ERROR_CODE = 11;
    public static final int UNEXPECTED_ERROR_CODE = 99;

    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<List<APIError>> handleRequestNotFoundException(RequestNotFoundException e) {
        log.warn(e.getMessage(), e);
        return new ResponseEntity<>(Collections.singletonList(new APIError(REQUEST_NOT_FOUND_ERROR_CODE, null, "request not found")), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MandatoryDataMissingException.class)
    public ResponseEntity<List<APIError>> handleMandatoryFieldMissingException(MandatoryDataMissingException e) {
        log.warn(e.getMessage(), e);
        return new ResponseEntity<>(Collections.singletonList(new APIError(MANDATORY_FIELD_MISSING_ERROR_CODE, e.getFieldName(), e.getMessage())), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FieldTooLongException.class)
    public ResponseEntity<List<APIError>> handleFieldTooLongException(FieldTooLongException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(Collections.singletonList(new APIError(FIELD_TOO_LONG_ERROR_CODE, e.getFieldName(), e.getMessage())), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCommandException.class)
    public ResponseEntity<List<APIError>> handleInvalidCommandException(InvalidCommandException e) {
        log.warn(e.getMessage(), e);
        return new ResponseEntity<>(Collections.singletonList(new APIError(INVALID_COMMAND_ERROR_CODE, e.getFieldName(), e.getMessage())), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RequestStateTransitionNotAllowed.class)
    public ResponseEntity<List<APIError>> handleRequestStateTransitionNotAllowed(RequestStateTransitionNotAllowed e) {
        log.warn(e.getMessage(), e);
        return new ResponseEntity<>(Collections.singletonList(new APIError(STATE_TRANSITION_NOT_ALLOWED_ERROR_CODE, "status", e.getMessage())), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OperationNotAllowedInCurrentState.class)
    public ResponseEntity<List<APIError>> handleOperationNotAllowedInCurrentState(OperationNotAllowedInCurrentState e) {
        log.warn(e.getMessage(), e);
        return new ResponseEntity<>(Collections.singletonList(new APIError(OPERATION_NOT_ALLOWED_ERROR_CODE, "status", e.getMessage())), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(StaleRequestVersionException.class)
    public ResponseEntity<List<APIError>> handleStaleRequestVersionException(StaleRequestVersionException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(Collections.singletonList(new APIError(STALE_VERSION_ERROR_CODE, "version", e.getMessage())), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<List<APIError>> handleParameterTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("unreadable request parameter {}: {}", e.getName(), e.getValue());
        return new ResponseEntity<>(Collections.singletonList(new APIError(MALFORMED_EXCEPTION_ERROR_CODE, e.getName(), "malformed request")), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RequestException.class)
    public ResponseEntity<List<APIError>> handleRequestException(RequestException e) {
        log.error("unmapped request exception, answering 400: {}", e.getMessage(), e);
        return new ResponseEntity<>(Collections.singletonList(new APIError(MALFORMED_EXCEPTION_ERROR_CODE, null, "malformed request")), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<List<APIError>> handleUnexpectedException(Exception e) {
        log.error("unexpected error while serving a request", e);
        return new ResponseEntity<>(Collections.singletonList(new APIError(UNEXPECTED_ERROR_CODE, null, "unexpected error")), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        log.warn("{} answered with {}: {}", e.getClass().getSimpleName(), statusCode, e.getMessage());
        return new ResponseEntity<>(Collections.singletonList(apiErrorFor(e)), headers, statusCode);
    }

    private static APIError apiErrorFor(Exception e) {
        if (e instanceof HttpMediaTypeNotSupportedException) {
            return new APIError(UNSUPPORTED_MEDIA_TYPE_ERROR_CODE, null, "request body must be " + MediaType.APPLICATION_JSON_VALUE);
        }
        if (e instanceof HttpRequestMethodNotSupportedException) {
            return new APIError(METHOD_NOT_ALLOWED_ERROR_CODE, null, "method not allowed");
        }
        if (e instanceof MissingServletRequestParameterException missing) {
            return new APIError(MALFORMED_EXCEPTION_ERROR_CODE, missing.getParameterName(), "malformed request");
        }
        if (e instanceof NoResourceFoundException || e instanceof NoHandlerFoundException) {
            return new APIError(UNKNOWN_ENDPOINT_ERROR_CODE, null, "unknown endpoint");
        }
        return new APIError(MALFORMED_EXCEPTION_ERROR_CODE, null, "malformed request");
    }
}
