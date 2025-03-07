package breakable.toy2.spotify_app;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

        private String refreshToken = "";

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

                ResponseEntity<Map<String, Object>> apiResponse = restTemplate.exchange(url, HttpMethod.POST, request,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                sessionCode = (String) apiResponse.getBody().get("access_token");
                refreshToken = (String) apiResponse.getBody().get("refresh_token");

                responseHeaders.add("Location", "http://localhost:5173/dashboard");
                responseBody.put("message", "ok");
                return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.FOUND);
        }

        @Scheduled(initialDelay = 55, fixedRate = 55, timeUnit = TimeUnit.MINUTES)
        private void refreshAccessToken() {
                if (refreshToken == null || refreshToken.isEmpty()) {
                        return;
                }
                String url = "https://accounts.spotify.com/api/token";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.setBasicAuth(clientId, clientSecret);

                String body = "grant_type=refresh_token&refresh_token=" + refreshToken;
                HttpEntity<String> request = new HttpEntity<>(body, headers);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, request,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                sessionCode = (String) response.getBody().get("access_token");

                if (response.getBody().get("refresh_token") != null) {
                        refreshToken = (String) response.getBody().get("refresh_token");
                }
        }

        @GetMapping("/me/top/artists")
        public Map<String, Object> getUserTopArtist() {
                String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/me/top/artists")
                                .queryParam("type", "artists")
                                .queryParam("limit", 10)
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

        @GetMapping("/artists/{id}")
        public Map<String, Object> getArtist(@PathVariable String id) {
                int limit = 5;
                String artistEndpoint = "https://api.spotify.com/v1/artists/";
                String url = UriComponentsBuilder.fromUriString(artistEndpoint + id).build()
                                .toString();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + sessionCode);
                headers.setContentType(MediaType.APPLICATION_JSON);

                ResponseEntity<Map<String, Object>> apiRresponse = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("artistDetails", apiRresponse.getBody());

                url = UriComponentsBuilder.fromUriString(artistEndpoint + id + "/top-tracks").build().toString();
                apiRresponse = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                response.put("artistTopTracks", apiRresponse.getBody());

                url = UriComponentsBuilder.fromUriString(artistEndpoint + id + "/albums").queryParam("limit", limit)
                                .build()
                                .toString();
                apiRresponse = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                response.put("artistAlbums", apiRresponse.getBody());

                return response;
        }

        @GetMapping("/albums/{id}")
        public Map<String, Object> getAlbum(@PathVariable String id) {
                String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/albums/" + id).build()
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

        @GetMapping("/playlists/{id}")
        public Map<String, Object> getPlaylist(@PathVariable String id) {
                String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/playlists/" + id).build()
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

        @GetMapping("/tracks/{id}")
        public Map<String, Object> getTrackDetails(@PathVariable String id) {
                String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/tracks/" + id).build()
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

        @GetMapping("/token")
        public ResponseEntity<String> getAuthToken() {

                return ResponseEntity.ok(sessionCode);
        }

        @GetMapping("/search")
        public Map<String, Object> search(@RequestParam String query) {
                String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/search")
                                .queryParam("q", query)
                                .queryParam("type", "album,artist,track,playlist")
                                .queryParam("limit", 5)
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
