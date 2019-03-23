package cc.ryanc.halo.listener;

import cc.ryanc.halo.config.properties.HaloProperties;
import cc.ryanc.halo.model.enums.BlogProperties;
import cc.ryanc.halo.model.support.HaloConst;
import cc.ryanc.halo.model.support.Theme;
import cc.ryanc.halo.service.OptionService;
import cc.ryanc.halo.utils.HaloUtils;
import cc.ryanc.halo.utils.ThemeUtils;
import cc.ryanc.halo.web.controller.content.base.BaseContentController;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateModelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cc.ryanc.halo.model.support.HaloConst.DEFAULT_THEME_NAME;

/**
 * <pre>
 *     应用启动完成后所执行的方法
 * </pre>
 *
 * @author : RYAN0UP
 * @date : 2018/12/5
 */
@Slf4j
@Configuration
public class StartedListener implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private freemarker.template.Configuration configuration;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private HaloProperties haloProperties;

    @Autowired
    private OptionService optionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        // save halo version to database
        this.cacheThemes();
        this.cacheOwo();
        this.getActiveTheme();
        this.printStartInfo();
    }

    /**
     * Cache themes to map
     */
    private void cacheThemes() {
        final List<Theme> themes = ThemeUtils.getThemes();
        if (null != themes) {
            HaloConst.THEMES = themes;
        }
    }

    /**
     * Get active theme
     */
    private void getActiveTheme() {
        BaseContentController.THEME = optionService.getByProperty(BlogProperties.THEME).orElse(DEFAULT_THEME_NAME);

        try {
            configuration.setSharedVariable("themeName", BaseContentController.THEME);
        } catch (TemplateModelException e) {
            e.printStackTrace();
        }
    }

    private void printStartInfo() {
        String blogUrl = getBaseUrl();

        log.info("Halo started at    {}", blogUrl);
        // TODO admin may be changeable
        log.info("Halo admin is at   {}/admin", blogUrl);
        if (!haloProperties.getDocDisabled()) {
            log.debug("Halo doc enable at {}/swagger-ui.html", blogUrl);
        }
    }

    /**
     * Gets blog url.
     *
     * @return blog url (If blog url isn't present, current machine IP address will be default)
     */
    private String getBaseUrl() {
        // Get server port
        String serverPort = applicationContext.getEnvironment().getProperty("server.port", "8080");

        String blogUrl = optionService.getByPropertyOfNullable(BlogProperties.BLOG_URL);

        if (StrUtil.isNotBlank(blogUrl)) {
            blogUrl = StrUtil.removeSuffix(blogUrl, "/");
        } else {
            blogUrl = String.format("http://%s:%s", HaloUtils.getMachineIP(), serverPort);
        }

        return blogUrl;
    }

    /**
     * Cache Owo
     */
    private void cacheOwo() {
        try {
            // The Map is LinkedHashMap
            @SuppressWarnings("unchecked")
            Map<String, String> owoMap = objectMapper.readValue(ResourceUtils.getURL("classpath:static/halo-common/OwO/OwO.path.json"), Map.class);

            HaloConst.OWO_MAP = Collections.unmodifiableMap(owoMap);
        } catch (IOException e) {
            log.error("Failed to read owo json", e);
            // TODO Consider to throw an exception
        }
    }
}
