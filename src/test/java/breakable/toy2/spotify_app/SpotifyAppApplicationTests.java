package breakable.toy2.spotify_app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpotifyAppApplicationTests {

	@Value("${spotify.client_id}")
	private String clientId;

	@Value("${spotify.redirect_uri}")
	private String redirectUri;

	@Value("${spotify.scope}")
	private String scope;

	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void shouldReturnAuthUrl() {
		ResponseEntity<String> response = restTemplate.postForEntity("/auth/spotify", null, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		String authUrl = response.getBody();
		assertThat(authUrl).isNotEmpty();
	}

	@Test
	void shouldForbidInvalidState() {
		String url = UriComponentsBuilder.fromUriString("/auth/spotify")
				.queryParam("code", "abc123")
				.queryParam("state", UUID.randomUUID().toString())
				.build()
				.toString();

		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

}
