package dev.byteide.runner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RunnerConfig {

    @Bean
    StudentLibraries studentLibraries() {
        return StudentLibraries.shared();
    }
}
