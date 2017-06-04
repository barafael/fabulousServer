import java.io.Serializable;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 04.06.17.
 */
public class LoginRequest implements Serializable{

    private String loginName;
    private String password;

    public LoginRequest(String loginName, String password) {
        this.loginName = loginName;
        this.password = password;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        //TODO: validate String
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        //TODO: validate String
        this.password = password;
    }
}
