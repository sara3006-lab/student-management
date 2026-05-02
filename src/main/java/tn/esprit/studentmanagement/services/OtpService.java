package tn.esprit.studentmanagement.services;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    private final Map<String, String> otpStore = new HashMap<>();
    private final Map<String, Long> otpTimestamp = new HashMap<>();
    private static final long OTP_EXPIRY_MS = 5 * 60 * 1000;

    public String generateOtp(String email) {
        String otp = String.format("%06d",
            new Random().nextInt(999999));
        otpStore.put(email, otp);
        otpTimestamp.put(email, System.currentTimeMillis());
        return otp;
    }

    public boolean validateOtp(String email, String otp) {
        if (!otpStore.containsKey(email)) return false;

        long elapsed = System.currentTimeMillis()
            - otpTimestamp.get(email);
        if (elapsed > OTP_EXPIRY_MS) {
            otpStore.remove(email);
            otpTimestamp.remove(email);
            return false;
        }

        boolean valid = otpStore.get(email).equals(otp);
        if (valid) {
            otpStore.remove(email);
            otpTimestamp.remove(email);
        }
        return valid;
    }
}
