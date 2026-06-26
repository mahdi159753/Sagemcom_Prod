package com.alibou.security.ligneproduction;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LigneProductionSeeder {

    @Bean
    public CommandLineRunner initFixedLines(LigneProductionRepository repository) {
        return args -> {
            // Check if the database has any lines
            if (repository.count() == 0) {
                // Seed exactly 8 fixed production lines
                for (int i = 1; i <= 8; i++) {
                    repository.save(
                        LigneProduction.builder()
                            .code("LIG-00" + i)
                            .nom("Ligne " + i)
                            .statut(LigneStatut.EN_PRODUCTION)
                            .cadenceObjectif(200)
                            .cadenceReelle(0)
                            .trg(0.0)
                            .trs(0.0)
                            .fpy(0.0)
                            .build()
                    );
                }
                System.out.println("✅ Automatically seeded 8 static production lines into the database.");
            }
        };
    }
}
