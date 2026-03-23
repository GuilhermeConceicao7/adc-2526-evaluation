package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.cloud.datastore.*;
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
import pt.unl.fct.di.adc.firstwebapp.util.Responses.UserRoleResponse;

@Path("/ShowUserRole")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUserRoleResource {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");


    public ShowUserRoleResource() {
    }

    public static class ShowUserRoleRequest {
        public Input input;
        public AuthToken token;
    }

    public static class Input {
        public String userId;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showRole(ShowUserRoleRequest data, @Context HttpServletRequest request, @Context HttpHeaders headers) {

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

        try {
            String targetUserId = data.input.userId;
            Key userKey = userKeyFactory.newKey(targetUserId);
            Entity user = datastore.get(userKey);

            if (user == null) {
                ApiError error = new ApiError("USER_NOT_FOUND", 9902,
                        "The username referred in the operation doesn’t exist in registered accounts");
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }

            String targetRole = user.getString("role");


            UserRoleResponse response = new UserRoleResponse(targetUserId, targetRole);
            return Response.ok(response).build();

        } catch (Exception e) {

            ApiError error = new ApiError("FORBIDDEN", 9907,
                    "The operation generated a forbidden error by other reason");
            return Response.status(Response.Status.FORBIDDEN).entity(error).build();
        }
    }

}