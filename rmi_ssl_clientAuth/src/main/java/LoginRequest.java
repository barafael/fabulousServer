import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 04.06.17.
 */
class LoginRequest implements Serializable {

    private String loginName;
    private String password;

    public LoginRequest(String loginName, String password) {
        this.loginName = loginName;
        this.password = password;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getPassword() {
        return password;
    }

    public boolean setLoginName(@NotNull String loginName) {
        if (loginName.length() >= 2 && loginName.length() <= 100) {
            this.loginName = loginName;
            return true;
        }
        return false;
    }

    public boolean setPassword(@NotNull String password) {
        String pattern = "\\A(?=\\S*?[0-9])(?=\\S*?[a-z])(?=\\S*?[A-Z])(?=\\S*?[@#$%^&+=])\\S{8,}\\z";
        /* Explanations:
               https://stackoverflow.com/questions/3802192/regexp-java-for-password-validation
               https://stackoverflow.com/a/3802238/5226836
               https://stackoverflow.com/a/32649219/5226836
        \A                # start-of-string
        (?=.*[0-9])       # a digit must occur at least once
        (?=.*[a-z])       # a lower case letter must occur at least once
        (?=.*[A-Z])       # an upper case letter must occur at least once
        (?=.*[@#$%^&+=])  # a special character must occur at least once
        (?=\S+$)          # no whitespace allowed in the entire string
        .{8,}             # anything, at least eight places though
        \z                # end-of-string
        */
        if (password.matches(pattern)) {
            this.password = password;
            return true;
        }
        System.err.println("WARNING: Password did not contain necessary characters." +
                "If you see this warning in production, it is a bug. Please report.");
        this.password = password; //TODO remove this line eventually
        return false;
    }
}
