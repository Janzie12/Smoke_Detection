import java.util.List;

@Dao
public interface UserInfoDao {
    @Insert
    void insertUserInfo(UserInfo userInfo);

    @Update
    void updateUserInfo(UserInfo userInfo);

    @Query("SELECT * FROM UserInfo WHERE id = :id")
    UserInfo getUserInfo(int id);
}

@Dao
public interface RecipientDao {
    @Insert
    void insertRecipient(Recipient recipient);

    @Query("SELECT * FROM Recipient")
    List<Recipient> getAllRecipients();
}
