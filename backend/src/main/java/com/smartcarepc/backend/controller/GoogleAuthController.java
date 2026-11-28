package com.smartcarepc.backend.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import com.smartcarepc.backend.service.EmailProofs;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {
    private static final String CLIENT_ID = "666308491003-lasilr6p89lq8b06q6v87tmi2bb29o84.apps.googleusercontent.com";
    private final RestClient client = RestClient.create("https://oauth2.googleapis.com");
    private final EmailProofs proofs;

    public GoogleAuthController(EmailProofs proofs) { this.proofs = proofs; }

    record Credential(String credential) {}
    /** emailProof: Google đã xác minh email này, dùng để bật email nhắc bảo dưỡng mà không cần thư xác nhận */
    record User(String email, String name, String emailProof) {}

    @PostMapping("/google")
    public User google(@RequestBody Credential body) {
        if (body.credential() == null || body.credential().isBlank() || body.credential().length() > 8192)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = client.post().uri("/tokeninfo")
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body("id_token=" + java.net.URLEncoder.encode(body.credential(), java.nio.charset.StandardCharsets.UTF_8))
                    .retrieve().body(Map.class);
            if (claims == null || !CLIENT_ID.equals(claims.get("aud")) || !"true".equals(String.valueOf(claims.get("email_verified")))
                    || !(claims.get("email") instanceof String email))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            String name = claims.get("name") instanceof String n ? n : email;
            return new User(email, name, proofs.issue(email));
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google authentication failed"); }
    }
}
