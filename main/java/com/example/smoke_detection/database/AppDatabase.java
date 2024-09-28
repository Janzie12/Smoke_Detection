import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {UserInfo.class, Recipient.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserInfoDao userInfoDao();
    public abstract RecipientDao recipientDao();
}
