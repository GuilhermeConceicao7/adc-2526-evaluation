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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("/Logout")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LogoutResource {

    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.newBuilder()
            .setProjectId("adc-evaluation-65595")
            .build()
            .getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public LogoutResource() {
    }

    public static class LogoutRequest {
        public Input input;
        public AuthToken token;
    }

    public static class Input {
        public String userId;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showRole(LogoutRequest data, @Context HttpServletRequest request, @Context HttpHeaders headers) {

        AuthToken token = data.token;

        if (token == null || token.getTokenId() == null || token.getTokenId().isBlank()) {
            ApiError error = new ApiError("INVALID_TOKEN", 9903,
                    "The operation is called with an invalid token (wrong format for example)");
            return Response.status(Response.Status.FORBIDDEN).entity(error).build();
        }

        Key sessionKey = datastore.newKeyFactory().setKind("UserSession").newKey(token.getTokenId());

        Transaction txn = datastore.newTransaction();

        try {
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
            String usernameSession = session.getString("username");
            String username = data.input.userId;

            Key userKey = userKeyFactory.newKey(username);
            Entity user = txn.get(userKey);

            if (user == null) {
                ApiError error = new ApiError("USER_NOT_FOUND", 9902,
                        "The username referred in the operation doesn’t exist in registered accounts");
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }

            if (!role.equals("ADMIN") && !username.equals(usernameSession)) {
                ApiError error = new ApiError("UNAUTHORIZED", 9905,
                        "The operation is not allowed for the user role");
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }

            if (role.equals("ADMIN")) {
                Query<Entity> query = Query.newEntityQueryBuilder()
                        .setKind("UserSession")
                        .setFilter(StructuredQuery.PropertyFilter.eq("username", username))
                        .build();

                QueryResults<Entity> results = txn.run(query);

                List<Key> keysToDelete = new ArrayList<>();

                results.forEachRemaining(entity -> {
                    keysToDelete.add(entity.getKey());
                });

                txn.delete(keysToDelete.toArray(new Key[0]));

            } else if (username.equals(usernameSession)) {

                txn.delete(sessionKey);

            }
            txn.commit();

            GenericResponse response = new GenericResponse("sucess", "Logout successful");
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
