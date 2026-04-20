import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class test_bcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS0/WhObS5l1Kx.Xu";

        String[] passwords = {
            "admin", "123456", "password", "admin123", "admin888",
            "root", "zyy123", "zyy123456", "admin123456",
            "admin0", "admin1", "Admin123", "Administrator",
            "test", "test1234", "system", "manager",
            "qwer1234", "password1", "pass1234", "P@ssw0rd",
            "zyy888", "zyy666", "admin666", "admin999",
            "123456a", "123456abc", "admin@123", "admin#123",
            "Admin123456", "administrator", "admin12345",
            "888888", "666666", "000000",
            "qwer", "asdf", "zxcv",
            "1234qwer", "1qaz2wsx", "Qwer1234",
            "Aa123456", "Aa1234",
        };
        for (String p : passwords) {
            boolean match = encoder.matches(p, hash);
            System.out.println(p + " -> " + match);
            if (match) {
                System.out.println("*** FOUND: " + p);
                break;
            }
        }
    }
}
