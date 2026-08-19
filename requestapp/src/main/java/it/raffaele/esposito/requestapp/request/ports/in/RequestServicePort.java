package it.raffaele.esposito.requestapp.request.ports.in;

import it.raffaele.esposito.requestapp.request.ports.in.dto.in.NewRequestCommand;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.RequestUpdateStatusWithReason;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.RequestUpdateBodyCommand;
import it.raffaele.esposito.requestapp.request.ports.in.dto.out.RequestData;

public interface RequestServicePort {

    RequestData getRequestById(String requestId);

    String createRequest(NewRequestCommand newRequestCommand);

    void updateRequestBody(RequestUpdateBodyCommand requestUpdateBodyCommand, long version);

    void verify(String requestId, long version);

    void accept(String requestId, long version);

    void reject(RequestUpdateStatusWithReason requestUpdateStatusWithReason, long version);

    String publish(String requestId, long version);

    void delete(RequestUpdateStatusWithReason requestUpdateStatusWithReason, long version);
}
