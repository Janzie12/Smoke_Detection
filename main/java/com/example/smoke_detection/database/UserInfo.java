@Entity
public class UserInfo {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String email;
    public String address;
    public String phone;
}

@Entity
public class Recipient {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String email;
}
