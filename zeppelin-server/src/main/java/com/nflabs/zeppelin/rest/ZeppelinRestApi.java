package com.nflabs.zeppelin.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 *
 *
 * @author anthonycorbacho
 * @since 0.3.4
 */
@Path("/")
public class ZeppelinRestApi {

  /**
   * Required by Swagger
   */
  public ZeppelinRestApi() {
    super();
  }

  /**
   * Get the root endpoint
   * Return always 200
   *
   * @return 200 response
   */
  @GET
  public Response getRoot(){
    return Response.ok().build();
  }
}
