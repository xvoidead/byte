package dev.byteide.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
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
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].slug").value("hello-world"))
                .andExpect(jsonPath("$[0].theory").doesNotExist());
    }

    @Test
    void returnsLessonWithoutHiddenTests() throws Exception {
        mvc.perform(get("/api/lessons/input"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Ввод с клавиатуры"))
                .andExpect(jsonPath("$.examples", hasSize(2)))
                .andExpect(jsonPath("$.testCount").value(3))
                .andExpect(jsonPath("$.prev").value("variables"))
                .andExpect(jsonPath("$.next").value("conditions"));
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
    void forwardsClientRoutesToSpa() throws Exception {
        mvc.perform(get("/lessons/loops")).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/playground")).andExpect(forwardedUrl("/index.html"));
    }
}
