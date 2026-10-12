
package com.example.LoanMatrix.service.authentication;

import com.example.LoanMatrix.dto.authentication.CustomerRegistrationRequest;
import com.example.LoanMatrix.dto.authentication.LoginRequest;
import com.example.LoanMatrix.dto.authentication.OtpVerificationRequest;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.Role;
import com.example.LoanMatrix.entity.User;
import com.example.LoanMatrix.repository.CustomerRepository;
import com.example.LoanMatrix.repository.authentication.RoleRepository;
import com.example.LoanMatrix.repository.UserRepository;
import com.example.LoanMatrix.response.LoginResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Locale;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(
            CustomerRepository customerRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JwtService jwtService,
            @Value("${google.client-id}") String googleClientId)
            throws GeneralSecurityException, IOException {

        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;

        this.googleIdTokenVerifier =
                new GoogleIdTokenVerifier.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance()
                )
                        .setAudience(
                                Collections.singletonList(googleClientId)
                        )
                        .build();
    }

    @Transactional
    public Customer registerCustomer(
            CustomerRegistrationRequest request) {

        // Normalize values before checking and saving them.
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        String aadhaarNo = request.getAadhaarNo().trim();

        String panCard = request.getPanCard()
                .trim()
                .toUpperCase(Locale.ROOT);

        // Check whether the email already exists.
        if (userRepository.existsByEmail(email)
                || customerRepository.existsByEmail(email)) {

            throw new RuntimeException("Email already registered");
        }

        // Check whether the Aadhaar number already exists.
        if (customerRepository.existsByAadhaarNo(aadhaarNo)) {
            throw new RuntimeException(
                    "Aadhaar number already registered"
            );
        }

        // Check whether the PAN number already exists.
        if (customerRepository.existsByPanCard(panCard)) {
            throw new RuntimeException(
                    "PAN number already registered"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        Customer customer = new Customer();

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(email);
        customer.setPassword(hashedPassword);
        customer.setMobileNo(request.getMobileNo());
        customer.setAadhaarNo(aadhaarNo);
        customer.setPanCard(panCard);
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setEmploymentType(request.getEmploymentType());
        customer.setMonthlyIncome(request.getMonthlyIncome());
        customer.setMonthlyInvestmentAmount(
                request.getMonthlyInvestmentAmount()
        );
        customer.setCreatedAt(LocalDateTime.now());

        Customer savedCustomer = customerRepository.save(customer);

        Role role = roleRepository.findByRoleName("CUSTOMER");

        if (role == null) {
            throw new RuntimeException("Customer role not found");
        }

        User user = new User();

        user.setRole(role);
        user.setCustomer(savedCustomer);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(email);
        user.setMobile(request.getMobileNo());
        user.setPassword(hashedPassword);
        user.setEmailVerified(false);

        // Generate a six-digit OTP.
        String otp = String.valueOf(
                (int) (Math.random() * 900000) + 100000
        );

        // Store the hashed OTP, not the original OTP.
        String otpHash = passwordEncoder.encode(otp);

        user.setOtp(otpHash);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        // EmailService will send this asynchronously when @Async
        // and @EnableAsync are configured in the project.
        emailService.sendOtpEmail(user.getEmail(), otp);

        return savedCustomer;
    }

    public void verifyOtp(OtpVerificationRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (user.getOtpExpiry() == null
                || user.getOtp() == null
                || user.getOtpExpiry().isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "OTP has expired or is unavailable"
            );
        }

        boolean otpMatches = passwordEncoder.matches(
                request.getOtp(),
                user.getOtp()
        );

        if (!otpMatches) {
            throw new RuntimeException("Invalid OTP");
        }

        user.setEmailVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException(
                    "Please verify your email first"
            );
        }

        String token = jwtService.generateToken(user.getEmail());

        return LoginResponse.builder()
                .token(token)
                .build();
    }

    @Transactional
    public LoginResponse googleLogin(String idTokenValue) {


        GoogleIdToken googleIdToken;

        try {
            googleIdToken = googleIdTokenVerifier.verify(idTokenValue);
        }
        catch (GeneralSecurityException | IOException e) {
            System.err.println("Google token verification error: "
                    + e.getClass().getSimpleName());
            e.printStackTrace();

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unable to verify Google ID token"
            );
        }

        if (googleIdToken == null) {
            System.err.println(
                    "Google token verification returned null. Check token expiry, issuer and audience."
            );

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired Google ID token"
            );
        }


        GoogleIdToken.Payload payload =
                googleIdToken.getPayload();

        String email = payload.getEmail();

        if (email == null
                || !Boolean.TRUE.equals(payload.getEmailVerified())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Google email is not verified"
            );
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        // Only an existing LoanMatrix user can log in.
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No LoanMatrix account found for this Google email. " +
                            "Register first or ask the administrator to create the account."
            );
        }

        if (user.getRole() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No role is assigned to this account"
            );
        }

        // Google has verified ownership of this email.
        // Supports existing Customer and Loan Officer accounts.
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getEmail());

        return LoginResponse.builder()
                .token(token)
                .build();
    }
}
