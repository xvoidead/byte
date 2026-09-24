package dev.byteide.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listsLessons() throws Exception {
        mvc.perform(get("/api/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(32)))
                .andExpect(jsonPath("$[0].slug").value("hello-world"))
                .andExpect(jsonPath("$[0].theory").doesNotExist());
    }

    @Test
    void returnsLessonWithoutHiddenTests() throws Exception {
        mvc.perform(get("/api/lessons/input"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Ввод с клавиатуры"))
                .andExpect(jsonPath("$.examples", hasSize(2)))
                .andExpect(jsonPath("$.testCount").value(6))
                .andExpect(jsonPath("$.steps[0].title").value("Введение"))
                .andExpect(jsonPath("$.steps[*].blocks[*].type", org.hamcrest.Matchers.hasItem("quiz")))
                .andExpect(jsonPath("$.hints").isNotEmpty())
                .andExpect(jsonPath("$.solution").doesNotExist())
                .andExpect(jsonPath("$.prev").value("variables"))
                .andExpect(jsonPath("$.next").value("math"));
    }

    @Test
    void returnsReferenceSolutionSeparately() throws Exception {
        mvc.perform(get("/api/lessons/input/solution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", containsString("nextLong")));
    }

    @Test
    void firstLessonHasNoPrev() throws Exception {
        mvc.perform(get("/api/lessons/hello-world"))
                .andExpect(jsonPath("$.prev").value(nullValue()));
    }

    @Test
    void unknownLessonIs404() throws Exception {
        mvc.perform(get("/api/lessons/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Урок не найден"));
    }

    @Test
    void runsCode() throws Exception {
        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "public class Main { public static void main(String[] a) { System.out.print(new java.util.Scanner(System.in).nextInt() * 2); } }",
                         "stdin": "21"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.stdout").value("42"));
    }

    @Test
    void rejectsEmptyCode() throws Exception {
        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON).content("{\"code\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Код программы пуст."));
    }

    @Test
    void checkHidesDataOfHiddenTests() throws Exception {
        mvc.perform(post("/api/lessons/hello-world/check").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "public class Main { public static void main(String[] a) { System.out.println(\\"Привет, мир!\\"); } }"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.compiled").value(true))
                .andExpect(jsonPath("$.tests[0].passed").value(false))
                .andExpect(jsonPath("$.tests[0].actualOutput").value("Привет, мир!\n"));

        mvc.perform(post("/api/lessons/input/check").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "public class Main { public static void main(String[] a) { } }"}
                        """))
                .andExpect(jsonPath("$.tests[2].hidden").value(true))
                .andExpect(jsonPath("$.tests[2].stdin").value(nullValue()))
                .andExpect(jsonPath("$.tests[2].expectedOutput").value(nullValue()));
    }

    @Test
    void servesPagesWithLessonMetaTags() throws Exception {
        mvc.perform(get("/lessons/loops"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<title>Циклы: for и while — урок")))
                .andExpect(content().string(containsString("og:description")));
        mvc.perform(get("/playground")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>byte — Java в браузере</title>")));
    }

    @Test
    void unknownPagesAre404HtmlAndUnknownApiIs404Json() throws Exception {
        mvc.perform(get("/about"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Страница не найдена")));
        mvc.perform(get("/lessons/no-such-lesson")).andExpect(status().isNotFound());
        mvc.perform(get("/api/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Не найдено"));
    }

    @Test
    void addsSecurityHeaders() throws Exception {
        mvc.perform(get("/api/lessons"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void servesRobotsAndSitemap() throws Exception {
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sitemap: http://localhost/sitemap.xml")));
        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<loc>http://localhost/lessons/hello-world</loc>")));
    }

    @Test
    void reportsHealth() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void diagnosesCodeWithoutRunningIt() throws Exception {
        mvc.perform(post("/api/compile").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "public class Main { public static void main(String[] a) { int x = 5 } }"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostics[0].severity").value("ERROR"))
                .andExpect(jsonPath("$.diagnostics[0].code").value("compiler.err.expected"))
                .andExpect(jsonPath("$.diagnostics[0].hint").exists());
        mvc.perform(post("/api/compile").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "public class Main { static int f() { } }"}
                        """))
                .andExpect(jsonPath("$.diagnostics[0].code").value("compiler.err.missing.ret.stmt"));
    }

    @Test
    void rejectsHugeRequests() throws Exception {
        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"" + "x".repeat(600_000) + "\"}"))
                .andExpect(status().isPayloadTooLarge());
    }
}
