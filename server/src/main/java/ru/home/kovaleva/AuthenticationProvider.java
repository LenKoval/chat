package ru.home.kovaleva;

import java.util.List;

public interface AuthenticationProvider {
    String getUsernameByLoginAndPassword(String login, String password);
    boolean register(String login, String password, String username);
    boolean checkAccess(String str);
    String[] changeUsername(String nUsername);
    void changeBanUser(String ban);
    List<String> getBannedUsers();
}
