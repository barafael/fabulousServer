/**
 * Created by jo on 03.06.17.
 */
public class User {
    private String name;

    public User(String n){
        this.name = n;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
