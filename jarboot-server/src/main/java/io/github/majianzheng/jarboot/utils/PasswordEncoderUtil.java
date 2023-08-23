
package io.github.majianzheng.jarboot.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author majianzheng
 */
public class PasswordEncoderUtil {
    
    public static boolean matches(String raw, String encoded) {
        return new BCryptPasswordEncoder().matches(raw, encoded);
    }
    
    public static String encode(String raw) {
        return new BCryptPasswordEncoder().encode(raw);
    }
    private PasswordEncoderUtil() {}
}
