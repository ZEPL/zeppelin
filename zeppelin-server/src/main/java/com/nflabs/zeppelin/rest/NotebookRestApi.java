package com.nflabs.zeppelin.rest;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.nflabs.zeppelin.session.TicketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nflabs.zeppelin.interpreter.Interpreter;
import com.nflabs.zeppelin.interpreter.Interpreter.RegisteredInterpreter;
import com.nflabs.zeppelin.interpreter.InterpreterSetting;
import com.nflabs.zeppelin.notebook.Notebook;
import com.nflabs.zeppelin.rest.message.InterpreterSettingListForNoteBind;
import com.nflabs.zeppelin.server.JsonResponse;

/**
 * Rest api endpoint for the noteBook.
 */
@Path("/notebook")
@Produces("application/json")
public class NotebookRestApi {
  Logger logger = LoggerFactory.getLogger(NotebookRestApi.class);
  Gson gson = new Gson();
  private Notebook notebook;

  public NotebookRestApi() {}

  public NotebookRestApi(Notebook notebook) {
    this.notebook = notebook;
  }

  /**
   * bind a setting to note
   * @throws IOException 
   */
  @PUT
  @Path("interpreter/bind/{noteId}/{principal}/{ticket}")
  public Response bind(@PathParam("noteId") String noteId, @PathParam("principal") String principal, @PathParam("ticket") String ticket, String req) throws Exception {
    List<String> settingIdList = gson.fromJson(req, new TypeToken<List<String>>(){}.getType());
    if (!TicketContainer.instance.isValid(principal, ticket))
      throw new Exception("Invalid principal / ticket:" + principal + "/" + ticket);

    notebook.bindInterpretersToNote(noteId, settingIdList, principal);
    return new JsonResponse(Status.OK).build();
  }

  /**
   * list binded setting
   */
  @GET
  @Path("interpreter/bind/{noteId}/{principal}/{ticket}")
  public Response bind(@PathParam("noteId") String noteId, @PathParam("principal") String principal, @PathParam("ticket") String ticket) throws Exception {
    List<InterpreterSettingListForNoteBind> settingList
      = new LinkedList<>();

    if (!TicketContainer.instance.isValid(principal, ticket))
      throw new Exception("Invalid principal / ticket:" + principal + "/" + ticket);

    List<InterpreterSetting> selectedSettings = notebook.getBindedInterpreterSettings(noteId, principal);
    for (InterpreterSetting setting : selectedSettings) {
      settingList.add(new InterpreterSettingListForNoteBind(
          setting.id(),
          setting.getName(),
          setting.getGroup(),
          setting.getInterpreterGroup(),
          true)
      );
    }

    List<InterpreterSetting> availableSettings = notebook.getInterpreterFactory().get();
    for (InterpreterSetting setting : availableSettings) {
      boolean selected = false;
      for (InterpreterSetting selectedSetting : selectedSettings) {
        if (selectedSetting.id().equals(setting.id())) {
          selected = true;
          break;
        }
      }

      if (!selected) {
        settingList.add(new InterpreterSettingListForNoteBind(
            setting.id(),
            setting.getName(),
            setting.getGroup(),
            setting.getInterpreterGroup(),
            false)
        );
      }
    }
    return new JsonResponse(Status.OK, "", settingList).build();
  }
}
