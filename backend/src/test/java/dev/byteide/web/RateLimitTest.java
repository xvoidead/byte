package dev.byteide.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"byte.limits.run-burst=2", "byte.limits.runs-per-minute=1"})
@AutoConfigureMockMvc
class RateLimitTest {

    private static final String PROGRAM =
            "{\"code\": \"public class Main { public static void main(String[] a) { } }\"}";

    @Autowired
    MockMvc mvc;

    @Test
    void limitsRunsPerIp() throws Exception {
        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON).content(PROGRAM)).andExpect(status().isOk());
        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON).content(PROGRAM)).andExpect(status().isOk());
        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON).content(PROGRAM))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void limitsConcurrentRunsPerIp() {
        RateLimiter limiter = new RateLimiter(new LimitsProperties(30, 15, 150, 60, 2, 524288));
        org.assertj.core.api.Assertions.assertThat(limiter.acquireRun("1.2.3.4")).isTrue();
        org.assertj.core.api.Assertions.assertThat(limiter.acquireRun("1.2.3.4")).isTrue();
        org.assertj.core.api.Assertions.assertThat(limiter.acquireRun("1.2.3.4")).isFalse();
        org.assertj.core.api.Assertions.assertThat(limiter.acquireRun("5.6.7.8")).isTrue();
        limiter.releaseRun("1.2.3.4");
        org.assertj.core.api.Assertions.assertThat(limiter.acquireRun("1.2.3.4")).isTrue();
    }
}
