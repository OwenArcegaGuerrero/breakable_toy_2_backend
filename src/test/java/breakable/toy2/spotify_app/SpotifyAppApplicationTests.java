package breakable.toy2.spotify_app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
	void shouldReturnTopAuthors() {
		ResponseEntity<Map> response = restTemplate.getForEntity("/me/top/artists", Map.class);
		assertThat(response.getStatusCode()).isNotNull();

		assertThat(response.getBody().size()).isEqualTo(10);
	}

	@Test
	void shouldReturnAnArtist() {
		ResponseEntity<Map> response = restTemplate.getForEntity("/artists/0TnOYISbd1XYRBk9myaseg", Map.class);
		assertThat(response.getStatusCode()).isNotNull();
	}

	@Test
	void shouldReturnAnAlbum() {
		ResponseEntity<Map> response = restTemplate.getForEntity("/artists/4aawyAB9vmqN3uQ7FjRGTy", Map.class);
		assertThat(response.getStatusCode()).isNotNull();
	}

	@Test
	void shouldSearch() {
		ResponseEntity<Map> response = restTemplate
				.getForEntity("/search?query=remaster%20track:Doxy%20artist:Miles%20Davis", Map.class);
		assertThat(response.getStatusCode()).isNotNull();
	}
}
