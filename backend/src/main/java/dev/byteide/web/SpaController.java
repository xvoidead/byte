package dev.byteide.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Отдаёт index.html фронтенда для клиентских маршрутов, чтобы работали прямые ссылки и обновление страницы. */
@Controller
public class SpaController {

    @GetMapping({"/lessons/{slug}", "/playground"})
    public String index() {
        return "forward:/index.html";
    }
}
