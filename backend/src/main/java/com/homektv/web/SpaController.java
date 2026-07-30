package com.homektv.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 手机 H5 单页应用（Vue Router history 模式，base=/m/）托管。
 *
 * 二维码内容为 http://<NAS_IP>:8080/m?room=default（无尾斜杠），
 * 且 history 模式下深链接（/m/home、/m/queue 等）刷新会命中服务端，
 * 需统一转发到 /m/index.html 由前端路由接管。
 *
 * 静态资源 /m/assets/** 由 Spring 默认静态资源处理器（classpath:/static/）优先命中，
 * 不会走到这里；这里只兜底 SPA 页面路径。
 */
@Controller
public class SpaController {

    /** 服务根路径直接进入管理后台。 */
    @GetMapping("/")
    public String root() {
        return "redirect:/m/admin";
    }

    /** 二维码入口 /m（无尾斜杠）。 */
    @GetMapping("/m")
    public String entry() {
        return "forward:/m/index.html";
    }

    /**
     * SPA 深链接兜底。末段用 {p:[^\\.]+} 约束「不含点号」，天然排除静态资源
     * （/m/assets/index-xxx.js、*.css、*.png 等末段都带扩展名的请求），
     * 交给 Spring 默认静态处理器命中真实文件。
     *   /m/          → 首页
     *   /m/home      → 单段路由
     *   /m/artist/周杰伦、/m/admin/songs → 两段路由
     */
    @GetMapping({"/m/", "/m/{p:[^\\.]+}", "/m/{p:[^\\.]+}/{s:[^\\.]+}"})
    public String forward() {
        return "forward:/m/index.html";
    }
}
