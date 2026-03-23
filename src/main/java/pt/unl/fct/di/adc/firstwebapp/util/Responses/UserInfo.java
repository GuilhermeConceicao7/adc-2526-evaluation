package pt.unl.fct.di.adc.firstwebapp.util.Responses;

public class UserInfo {
    public String userId;
    public String username;
    public String email;
    public String role;

    public UserInfo(String userId, String username, String email, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }
}