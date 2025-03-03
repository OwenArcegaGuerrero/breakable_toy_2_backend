package breakable.toy2.spotify_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpotifyAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpotifyAppApplication.class, args);
	}
}
