package com.miraeasset.elibrary.config;

import com.miraeasset.elibrary.common.InvalidRequestException;
import com.miraeasset.elibrary.identity.Principal;
import com.miraeasset.elibrary.identity.Role;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class PrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Principal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String userId = webRequest.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new InvalidRequestException("INVALID_USER", USER_ID_HEADER + " must not be blank");
        }
        if (userId.length() > 100) {
            throw new InvalidRequestException("INVALID_USER", USER_ID_HEADER + " must not exceed 100 characters");
        }
        return new Principal(userId, resolveRole(webRequest.getHeader(ROLE_HEADER)));
    }

    private Role resolveRole(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            return Role.USER;
        }
        try {
            return Role.valueOf(roleHeader.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("INVALID_ROLE", ROLE_HEADER + " must be one of " + java.util.Arrays.toString(Role.values()));
        }
    }
}