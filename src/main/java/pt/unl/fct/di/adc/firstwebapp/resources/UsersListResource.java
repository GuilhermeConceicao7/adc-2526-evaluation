package pt.unl.fct.di.adc.firstwebapp.resources;


import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.firstwebapp.util.ApiError;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;
import pt.unl.fct.di.adc.firstwebapp.util.Responses.ShowUsersResponse;
import pt.unl.fct.di.adc.firstwebapp.util.Responses.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("/showUsers")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UsersListResource {

    private static final Logger LOG = Logger.getLogger(UsersListResource.class.getName());
    private static final Datastore datastore =  DatastoreOptions.newBuilder()
            .setProjectId("adc-evaluation-65595")
            .build()
            .getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    private final Gson g = new Gson();

    public UsersListResource() {
    }

    public static class ShowUsersRequest {
        public RegisterData.Input input;
        public AuthToken token;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLogin(ShowUsersRequest data, @Context HttpServletRequest request, @Context HttpHeaders headers) {

        AuthToken token = data.token;

        if (token == null || token.getTokenId() == null || token.getTokenId().isBlank()) {
            ApiError error = new ApiError("INVALID_TOKEN", 9903,
                    "The operation is called with an invalid token (wrong format for example)");
            return Response.status(Response.Status.FORBIDDEN).entity(error).build();
        }




            Key sessionKey = datastore.newKeyFactory().setKind("UserSession").newKey(token.getTokenId());
            Entity session = datastore.get(sessionKey);

            if (session == null) {
                ApiError error = new ApiError("INVALID_TOKEN", 9903,
                        "The operation is called with an invalid token (wrong format for example)");
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }

            long now = System.currentTimeMillis();
            if (session.getTimestamp("expiresAt").toDate().getTime() < now) {
                ApiError error = new ApiError("TOKEN_EXPIRED", 9904,
                        "The operation is called with a token that is expired");
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }

            String role = session.getString("role");
            if (!role.equals("ADMIN") && !role.equals("BOFFICER")) {
                ApiError error = new ApiError("UNAUTHORIZED", 9905,
                        "The operation is not allowed for the user role");
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .build();

            QueryResults<Entity> results = datastore.run(query);
        List<UserInfo> users = new ArrayList<>();

        results.forEachRemaining(u -> {
            users.add(new UserInfo(
                    u.getKey().getName(),
                    u.getString("user_name"),
                    u.getString("role")
            ));
        });

        ShowUsersResponse response = new ShowUsersResponse(users);

        return Response.ok(response).build();
        }

    }



