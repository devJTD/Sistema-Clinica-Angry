package com.clinica.sistema.Configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SeguridadConfiguracion implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new InterceptorSesion())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/registro", "/css/**", "/js/**", "/logout", "/error");
    }
}
