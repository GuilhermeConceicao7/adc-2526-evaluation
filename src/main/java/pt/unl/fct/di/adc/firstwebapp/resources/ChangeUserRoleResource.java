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
import pt.unl.fct.di.adc.firstwebapp.util.Responses.GenericResponse;

import java.util.logging.Logger;

@Path("/ChangeUserRole")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeUserRoleResource {

    private static final Logger LOG = Logger.getLogger(ChangeUserRoleResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public ChangeUserRoleResource() {
    }

    public static class ChangeRoleRequest {
        public Input input;
        public AuthToken token;
    }

    public static class Input {
        public String userId;
        public String newRole;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleRequest data, @Context HttpServletRequest request, @Context HttpHeaders headers) {

        Input input = data.input;
        AuthToken token = data.token;

        if (token == null || token.getTokenId() == null || token.getTokenId().isBlank()) {
            ApiError error = new ApiError("INVALID_TOKEN", 9903,
                    "The operation is called with an invalid token (wrong format for example)");
            return Response.status(Response.Status.FORBIDDEN).entity(error).build();
        }

        Key sessionKey = datastore.newKeyFactory()
                .setKind("UserSession")
                .newKey(token.getTokenId());

        Transaction txn = datastore.newTransaction();

        try {
            Entity session = txn.get(sessionKey);

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

            String targetUserId = input.userId;
            Key userKey = userKeyFactory.newKey(targetUserId);
            Entity user = txn.get(userKey);

            if (user == null) {
                ApiError error = new ApiError("USER_NOT_FOUND", 9902,
                        "The username referred in the operation doesn’t exist in registered accounts");
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }

            String requesterRole = session.getString("role");

            if (!requesterRole.equals("ADMIN")) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ApiError("UNAUTHORIZED", 9905, "The operation is not allowed for the user role"))
                        .build();
            }

            String newRole = input.newRole;

            if(!newRole.equals("ADMIN") && !newRole.equals("BOFFICER") && !newRole.equals("USER")){
                ApiError error = new ApiError("INVALID_CREDENTIALS", 9900,
                        "That role doesn't exist");
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }

            Entity updatedUser = Entity.newBuilder(user)
                    .set("role", newRole)
                    .build();

            txn.put(updatedUser);
            txn.commit();

            GenericResponse response = new GenericResponse("sucess", "Role updated successfully");
            return Response.ok(response).build();
        } catch (Exception e) {
            txn.rollback();
            LOG.severe(e.getMessage());
            ApiError error = new ApiError("FORBIDDEN", 9907,
                    "The operation generated a forbidden error by other reason");
            return Response.status(Response.Status.FORBIDDEN).entity(error).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}
