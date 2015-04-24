package com.nflabs.zeppelin.rest;

import com.nflabs.zeppelin.interpreter.InterpreterSetting;
import com.nflabs.zeppelin.rest.message.InterpreterSettingListForNoteBind;
import com.nflabs.zeppelin.server.JsonResponse;
import com.nflabs.zeppelin.session.TicketContainer;
import org.apache.shiro.SecurityUtils;
import scala.Tuple2;

import javax.security.auth.Subject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Zeppelin root rest api endpoint.
 *
 * @author anthonycorbacho
 * @since 0.3.4
 */
@Path("/security")
@Produces("application/json")
public class SecurityRestApi {
  /**
   * Required by Swagger.
   */
  public SecurityRestApi() {
    super();
  }

  /**
   * Get ticket
   *
   * @return 200 response
   */
  @GET
  @Path("ticket")
  public Response ticket() {
    String principal = SecurityUtils.getSubject().getPrincipal().toString();
    String ticket = TicketContainer.instance.getTicket(principal);
    Map<String, String> data = new HashMap<>();
    data.put("principal", principal);
    data.put("ticket", ticket);

    return new JsonResponse(Response.Status.OK, "", data).build();
  }
}
