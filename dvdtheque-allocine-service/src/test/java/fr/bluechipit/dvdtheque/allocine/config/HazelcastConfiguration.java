package fr.bluechipit.dvdtheque.allocine.config;

import com.hazelcast.config.AutoDetectionConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class HazelcastConfiguration {
	@Bean
	public HazelcastInstance hazelcastInstance() {
		Config config = new Config();
		config.getNetworkConfig().setJoin(new JoinConfig().setAutoDetectionConfig(new AutoDetectionConfig().setEnabled(false)));
		config.setInstanceName(RandomStringUtils.random(8, true, false))
				.addMapConfig(new MapConfig().setName("ficheFilms")).addMapConfig(new MapConfig().setName("ficheFilmsByTitle"));
		return Hazelcast.newHazelcastInstance(config);
	}
}
