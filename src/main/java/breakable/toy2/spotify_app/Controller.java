package breakable.toy2.spotify_app;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class Controller {

    @Value("${spotify.client_id}")
    private String clientId;

    @Value("${spotify.scope}")
    private String scope;

    @Value("${spotify.redirect_uri}")
    private String redirectUri;

    @Value("${spotify.client_secret}")
    private String clientSecret;

    private String originState = UUID.randomUUID().toString();

    private String sessionCode = "";

    private final RestTemplate restTemplate = new RestTemplate();

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
    public ResponseEntity<Map<String, String>> obtainAccessCode(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "state", required = false) String state) {

        HttpHeaders responseHeaders = new HttpHeaders();
        Map<String, String> responseBody = new HashMap<>();
        String homeUrl = "http://localhost:8080/";

        if (!originState.equals(state)) {
            responseHeaders.add("Location", homeUrl);
            responseBody.put("error", "bad request");
            return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (error != null) {
            responseHeaders.add("Location", homeUrl);
            responseBody.put("error", error);
            return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        String url = "https://accounts.spotify.com/api/token";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders apiHeaders = new HttpHeaders();
        apiHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        apiHeaders.setBasicAuth(clientId, clientSecret);

        String body = "grant_type=authorization_code&code=" + code + "&redirect_uri=" + redirectUri;
        HttpEntity<String> request = new HttpEntity<>(body, apiHeaders);

        ResponseEntity<Map> apiResponse = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
        sessionCode = (String) apiResponse.getBody().get("access_token");

        responseHeaders.add("Location", "http://localhost:5173/");
        responseBody.put("message", "ok");
        return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.FOUND);
    }

    @GetMapping("/me/top/artists")
    public Map getUserTopArtist() {
        String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/me/top/artists").build()
                .toString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sessionCode);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        return response.getBody();
    }

    @GetMapping("/artists/{id}")
    public Map getArtist(@RequestParam String artistId) {
        String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/artists/" + artistId).build()
                .toString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sessionCode);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        return response.getBody();
    }

    @GetMapping("/albums/{id}")
    public Map getAlbum(@RequestParam String albumId) {
        String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/albums/" + albumId).build()
                .toString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sessionCode);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        return response.getBody();
    }

    @GetMapping("/search")
    public Map search(@RequestParam String query) {
        String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/search")
                .queryParam("q", query)
                .build()
                .toString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sessionCode);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        return response.getBody();
    }

}
