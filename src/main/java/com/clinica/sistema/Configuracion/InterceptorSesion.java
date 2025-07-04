/*
package com.clinica.sistema.Configuracion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class InterceptorSesion implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        boolean usuarioLogueado = (session != null && session.getAttribute("usuario") != null);

        if (!usuarioLogueado) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
 */