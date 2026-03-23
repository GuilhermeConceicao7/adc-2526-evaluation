package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class UserSession {

    public static Entity create(Key key, AuthToken token) {
        return Entity.newBuilder(key)
                .set("username", token.getUserId())
                .set("role", token.getRole())
                .set("issuedAt", Timestamp.ofTimeSecondsAndNanos(token.getIssuedAt()/1000, 0))
                .set("expiresAt", Timestamp.ofTimeSecondsAndNanos(token.getExpiresAt()/1000, 0))
                .build();
    }
}