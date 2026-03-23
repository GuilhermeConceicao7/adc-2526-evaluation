package pt.unl.fct.di.adc.firstwebapp.util.Responses;

import java.sql.Timestamp;

public class SessionInfo {

    public String sessionId;
    public String username;
    public long expiresAt;
    public String role;

    public SessionInfo(String sessionId, String username, String role, long expiresAt) {
        this.sessionId = sessionId;
        this.username = username;
        this.role = role;
        this.expiresAt = expiresAt;
    }
}
