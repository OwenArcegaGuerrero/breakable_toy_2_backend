package breakable.toy2.spotify_app;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class Controller {

    @Value("${spotify.client_id}")
    private String clientId;

    @Value("${spotify.scope}")
    private String scope;

    @Value("${spotify.redirect_uri}")
    private String redirectUri;

    private String originState = UUID.randomUUID().toString();

    private String sessionCode = "";

    @PostMapping("/auth/spotify")
    public ResponseEntity<String> authorizeUser() {
        String authUrl = UriComponentsBuilder.fromUriString("https://accounts.spotify.com/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("scope", scope)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", originState)
                .build()
                .toUriString();

        return ResponseEntity.ok(authUrl);
    }

    @GetMapping("/auth/spotify")
    public ResponseEntity<String> obtainAccessCode(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "state", required = false) String state) {

        if (!originState.equals(state)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error, please try again.");
        }

        if (error != null) {
            return ResponseEntity.badRequest().body("Error" + error);
        }

        sessionCode = code;

        return ResponseEntity.ok("Authorization completed");
    }

}
