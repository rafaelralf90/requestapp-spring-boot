package it.raffaele.esposito.requestapp.adapter.in.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class APIError {
    private int code;
    private String fieldName;
    private String engMessage;
}
