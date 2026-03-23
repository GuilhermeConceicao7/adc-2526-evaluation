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
import pt.unl.fct.di.adc.firstwebapp.util.Responses.GenericResponse;

import java.util.Objects;
import java.util.logging.Logger;

@Path("/ModifyAccountAttributes")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UpdateAccountResource {

    private static final Logger LOG = Logger.getLogger(UpdateAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    private final Gson g = new Gson();

    public UpdateAccountResource() {
    }

    public static class UpdateRequest {
        public Input input;
        public AuthToken token;
    }

    public static class Input {
        public String userId;
        public Attributes attributes;
    }

    public static class Attributes {
        public String email;
        public String username;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccount(UpdateRequest data, @Context HttpServletRequest request, @Context HttpHeaders headers) {

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

            String requesterRole = session.getString("role");
            String requesterId = session.getString("username");

            String targetUserId = input.userId;
            String newEmail = input.attributes.email;
            String newUsername = input.attributes.username;

            if (!newUsername.equals("")) {
                ApiError error = new ApiError("INVALID_INPUT", 9906,
                        "The call is using input data not following the correct specification, username must be null");
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }

            Key userKey = userKeyFactory.newKey(targetUserId);
            Entity user = txn.get(userKey);


            if (user == null) {
                ApiError error = new ApiError("USER_NOT_FOUND", 9902,
                        "The username referred in the operation doesn’t exist in registered accounts");
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }


            String targetRole = user.getString("role");

            if (requesterRole.equals("USER") && !requesterId.equals(targetUserId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ApiError("UNAUTHORIZED", 9905, "Not allowed"))
                        .build();
            }

            if (requesterRole.equals("BOFFICER") && !requesterId.equals(targetUserId) && !targetRole.equals("USER")) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ApiError("UNAUTHORIZED", 9905, "Cannot modify this role"))
                        .build();
            }


            Entity updatedUser = Entity.newBuilder(user)
                    .set("user_email", newEmail)
                    .build();

            txn.put(updatedUser);
            txn.commit();

            GenericResponse response = new GenericResponse("sucess", "Updated successfully");
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