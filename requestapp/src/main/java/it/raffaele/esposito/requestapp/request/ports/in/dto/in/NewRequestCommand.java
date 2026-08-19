package it.raffaele.esposito.requestapp.request.ports.in.dto.in;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NewRequestCommand {

    private String name;
    private String body;
}
