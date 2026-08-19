package it.raffaele.esposito.requestapp.adapter.in.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewRequestPayload {

    private String name;
    private String body;
}
