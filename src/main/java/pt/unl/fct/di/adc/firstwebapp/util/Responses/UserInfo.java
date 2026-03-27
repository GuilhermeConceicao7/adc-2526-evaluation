package pt.unl.fct.di.adc.firstwebapp.util.Responses;

public class UserInfo {
    public String userId;
    public String username;
    public String role;

    public UserInfo(String userId, String username,  String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }
}