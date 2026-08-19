package it.raffaele.esposito.requestapp.request.ports.in.dto.in;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RequestUpdateBodyCommand {

    private String uuid;
    private String body;
}
