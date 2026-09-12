package com.realestate.shared.infrastructure.configuration;

import com.realestate.auth.infrastructure.web.DeviceIdResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final DeviceIdResolver devices;

    public WebConfiguration(DeviceIdResolver devices) {
        this.devices = devices;
    }

    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(devices);
    }
}
