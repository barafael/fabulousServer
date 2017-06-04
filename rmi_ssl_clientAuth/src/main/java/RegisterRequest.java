import java.io.Serializable;

/**
 * @author Johannes Köstler <github@johanneskoestler.de>
 * @date 03.06.17.
 */
class RegisterRequest implements Serializable {
    private String username;
    private String password;
    private String firstName;
    private String lastName;

    public RegisterRequest(String username, String password, String firstName, String lastName) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        //TODO: validate String
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        //TODO: validate String
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        //TODO: validate String
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        //TODO: validate String
        this.lastName = lastName;
    }
}
