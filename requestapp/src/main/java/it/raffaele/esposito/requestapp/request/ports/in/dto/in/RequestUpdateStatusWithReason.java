package it.raffaele.esposito.requestapp.request.ports.in.dto.in;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RequestUpdateStatusWithReason {

    private String uuid;
    private String reason;
}
