package com.musicplatform.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return (uri != null && uri.startsWith("/api/")) || (accept != null && accept.contains("application/json"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        log.warn("Resource not found: {} on URI: {}", ex.getMessage(), request.getRequestURI());
        if (isApiRequest(request)) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.NOT_FOUND.value());
            body.put("error", "Not Found");
            body.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.addObject("path", request.getRequestURI());
        return mav;
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public Object handleForbidden(ForbiddenOperationException ex, HttpServletRequest request, Model model) {
        log.warn("Forbidden operation: {} on URI: {}", ex.getMessage(), request.getRequestURI());
        if (isApiRequest(request)) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.FORBIDDEN.value());
            body.put("error", "Forbidden");
            body.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("message", ex.getMessage());
        mav.addObject("path", request.getRequestURI());
        return mav;
    }

    @ExceptionHandler({DuplicateResourceException.class, BusinessException.class, IllegalArgumentException.class})
    public Object handleBadRequest(RuntimeException ex, HttpServletRequest request, Model model) {
        log.warn("Bad request: {} on URI: {}", ex.getMessage(), request.getRequestURI());
        if (isApiRequest(request)) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.BAD_REQUEST.value());
            body.put("error", "Bad Request");
            body.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", ex.getMessage());
        mav.addObject("path", request.getRequestURI());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled error processing request: {}", request.getRequestURI(), ex);
        if (isApiRequest(request)) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            body.put("error", "Internal Server Error");
            body.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", "An unexpected error occurred. Our team has been notified.");
        mav.addObject("path", request.getRequestURI());
        return mav;
    }
}
