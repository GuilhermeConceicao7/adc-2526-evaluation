package pt.unl.fct.di.adc.firstwebapp.util.Responses;

import com.google.cloud.Timestamp;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;

public class LoginResponse {

    public String status;
    public Data data;

    public LoginResponse(AuthToken token) {
        this.status = "success";
        this.data = new Data(token);
    }

    public static class Data {
        public Token token;

        public Data(AuthToken token) {
            this.token = new Token(token);
        }
    }

    public static class Token {
        public String tokenId;
        public String userId;
        public String role;
        public Timestamp issuedAt;
        public Timestamp expiresAt;

        public Token(AuthToken token) {
            this.tokenId = token.getTokenId();
            this.userId = token.getUserId();
            this.role = token.getRole();
            this.issuedAt = Timestamp.ofTimeMicroseconds(token.getIssuedAt());
            this.expiresAt = Timestamp.ofTimeMicroseconds(token.getExpiresAt());
        }
    }
}