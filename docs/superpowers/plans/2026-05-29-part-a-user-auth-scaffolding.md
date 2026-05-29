# Part A: 用户与认证模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 Spring Boot 项目脚手架，实现用户注册/登录/认证体系、文件上传、前端骨架和部署配置，为其他成员提供可复用的公共基础。

**Architecture:** Spring Boot MVC 分层架构（Controller → Service → Mapper），JWT 无状态认证，STOMP over WebSocket 实时通信，前端静态资源放在 `src/main/resources/static/`。

**Tech Stack:** Spring Boot 3.x, MyBatis-Plus, JWT (jjwt), MySQL 8.0, HTML/CSS/JS, Docker

---

## 文件结构

```
backend/
├── pom.xml
├── src/main/java/com/ncu/chat/
│   ├── ChatApplication.java
│   ├── common/
│   │   ├── Result.java
│   │   ├── PageResult.java
│   │   └── GlobalExceptionHandler.java
│   ├── config/
│   │   ├── MyBatisPlusConfig.java
│   │   ├── WebMvcConfig.java
│   │   └── WebSocketConfig.java
│   ├── util/
│   │   ├── JwtUtil.java
│   │   └── FileUtil.java
│   ├── interceptor/
│   │   └── AuthInterceptor.java
│   ├── model/entity/
│   │   └── User.java
│   ├── model/dto/
│   │   ├── UserRegisterDTO.java
│   │   ├── UserLoginDTO.java
│   │   └── UserProfileDTO.java
│   ├── mapper/
│   │   └── UserMapper.java
│   ├── service/
│   │   ├── UserService.java
│   │   └── impl/UserServiceImpl.java
│   └── controller/
│       ├── AuthController.java
│       ├── UserController.java
│       └── FileController.java
├── src/main/resources/
│   ├── application.yml
│   └── static/
│   └── static/
│       ├── index.html
│       ├── login.html
│       ├── register.html
│       ├── profile.html
│       ├── css/
│       │   └── style.css
│       └── js/
│           ├── api.js
│           ├── auth.js
│           ├── websocket.js
│           └── utils.js
├── Dockerfile
├── docker-compose.yml
├── deploy.sh
└── .dockerignore
```

---

## Task 1: 初始化 Spring Boot 项目

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/ncu/chat/ChatApplication.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
    </parent>
    <groupId>com.ncu</groupId>
    <artifactId>chat-system</artifactId>
    <version>1.0.0</version>
    <name>chat-system</name>
    <description>NCU Web Homework - Online Chat System</description>
    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <jjwt.version>0.12.5</jjwt.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chat_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: your-256-bit-secret-key-here-change-in-production-ncu-chat-2026
  expiration: 86400000

file:
  upload:
    path: ./uploads
    max-size: 10MB
    allowed-types: jpg,jpeg,png,gif,pdf,doc,docx,txt,mp3,wav
```

- [ ] **Step 3: 创建主启动类**

```java
package com.ncu.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ncu.chat.mapper")
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建目录结构**

```bash
mkdir -p backend/src/main/java/com/ncu/chat/{common,config,util,interceptor,model/entity,model/dto,mapper,service/impl,controller}
mkdir -p backend/src/main/resources/{static/{css,js,assets},templates}
mkdir -p backend/src/test/java/com/ncu/chat
```

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat: 初始化 Spring Boot 项目结构和配置"
```

---

## Task 2: 公共基础类

**Files:**
- Create: `backend/src/main/java/com/ncu/chat/common/Result.java`
- Create: `backend/src/main/java/com/ncu/chat/common/PageResult.java`
- Create: `backend/src/main/java/com/ncu/chat/common/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建统一响应封装 Result.java**

```java
package com.ncu.chat.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    private Result() {}

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
```

- [ ] **Step 2: 创建分页结果 PageResult.java**

```java
package com.ncu.chat.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;

    public PageResult(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}
```

- [ ] **Step 3: 创建全局异常处理器 GlobalExceptionHandler.java**

```java
package com.ncu.chat.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException: {}", e.getMessage(), e);
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return Result.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        return Result.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        return Result.error("服务器内部错误");
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ncu/chat/common/
git commit -m "feat: 添加统一响应封装和全局异常处理"
```

---

## Task 3: 配置类

**Files:**
- Create: `backend/src/main/java/com/ncu/chat/config/MyBatisPlusConfig.java`
- Create: `backend/src/main/java/com/ncu/chat/config/WebMvcConfig.java`
- Create: `backend/src/main/java/com/ncu/chat/config/WebSocketConfig.java`

- [ ] **Step 1: 创建 MyBatisPlusConfig.java**

```java
package com.ncu.chat.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

- [ ] **Step 2: 创建 WebMvcConfig.java**

```java
package com.ncu.chat.config;

import com.ncu.chat.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
```

- [ ] **Step 3: 创建 WebSocketConfig.java**

```java
package com.ncu.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ncu/chat/config/
git commit -m "feat: 添加 MyBatis-Plus、MVC、WebSocket 配置"
```

---

## Task 4: JWT 工具类和认证拦截器

**Files:**
- Create: `backend/src/main/java/com/ncu/chat/util/JwtUtil.java`
- Create: `backend/src/main/java/com/ncu/chat/interceptor/AuthInterceptor.java`

- [ ] **Step 1: 创建 JwtUtil.java**

```java
package com.ncu.chat.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: 创建 AuthInterceptor.java**

```java
package com.ncu.chat.interceptor;

import com.ncu.chat.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\",\"data\":null}");
            return false;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\",\"data\":null}");
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        return true;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ncu/chat/util/ backend/src/main/java/com/ncu/chat/interceptor/
git commit -m "feat: 添加 JWT 工具类和认证拦截器"
```

---

## Task 5: User 实体和 Mapper

**Files:**
- Create: `backend/src/main/java/com/ncu/chat/model/entity/User.java`
- Create: `backend/src/main/java/com/ncu/chat/mapper/UserMapper.java`

- [ ] **Step 1: 创建 User 实体**

```java
package com.ncu.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String signature;
    private Integer status;      // 0-离线 1-在线 2-忙碌 3-勿扰
    private Integer role;        // 0-普通用户 1-管理员
    private Integer enabled;     // 0-禁用 1-启用
    @TableLogic
    private Integer deleted;     // 0-未删除 1-已删除
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime;
}
```

- [ ] **Step 2: 创建 UserMapper**

```java
package com.ncu.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.chat.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ncu/chat/model/ backend/src/main/java/com/ncu/chat/mapper/
git commit -m "feat: 添加 User 实体和 Mapper"
```

---

## Task 6: DTO 类

**Files:**
- Create: `backend/src/main/java/com/ncu/chat/model/dto/UserRegisterDTO.java`
- Create: `backend/src/main/java/com/ncu/chat/model/dto/UserLoginDTO.java`
- Create: `backend/src/main/java/com/ncu/chat/model/dto/UserProfileDTO.java`

- [ ] **Step 1: 创建 UserRegisterDTO**

```java
package com.ncu.chat.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少 6 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须包含字母和数字")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String avatar;
}
```

- [ ] **Step 2: 创建 UserLoginDTO**

```java
package com.ncu.chat.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 3: 创建 UserProfileDTO**

```java
package com.ncu.chat.model.dto;

import lombok.Data;

@Data
public class UserProfileDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String signature;
    private Integer status;
    private Integer role;
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ncu/chat/model/dto/
git commit -m "feat: 添加用户相关 DTO 类"
```

---

## Task 7: UserService 接口和实现

**Files:**
- Create: `backend/src/main/java/com/ncu/chat/service/UserService.java`
- Create: `backend/src/main/java/com/ncu/chat/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 创建 UserService 接口**

```java
package com.ncu.chat.service;

import com.ncu.chat.model.dto.UserLoginDTO;
import com.ncu.chat.model.dto.UserProfileDTO;
import com.ncu.chat.model.dto.UserRegisterDTO;
import com.ncu.chat.model.entity.User;

import java.util.Map;

public interface UserService {
    Map<String, Object> register(UserRegisterDTO dto);
    Map<String, Object> login(UserLoginDTO dto);
    UserProfileDTO getProfile(Long userId);
    UserProfileDTO updateProfile(Long userId, UserProfileDTO dto);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void updateStatus(Long userId, Integer status);
    User getUserById(Long userId);
}
```

- [ ] **Step 2: 创建 UserServiceImpl**

```java
package com.ncu.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.dto.UserLoginDTO;
import com.ncu.chat.model.dto.UserProfileDTO;
import com.ncu.chat.model.dto.UserRegisterDTO;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.service.UserService;
import com.ncu.chat.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> register(UserRegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("两次密码不一致");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        user.setStatus(0);
        user.setRole(0);
        user.setEnabled(1);
        user.setDeleted(0);
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", convertToProfile(user));
        return result;
    }

    @Override
    public Map<String, Object> login(UserLoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }
        if (user.getEnabled() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        user.setStatus(1);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", convertToProfile(user));
        return result;
    }

    @Override
    public UserProfileDTO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToProfile(user);
    }

    @Override
    public UserProfileDTO updateProfile(Long userId, UserProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getSignature() != null) user.setSignature(dto.getSignature());
        userMapper.updateById(user);
        return convertToProfile(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setStatus(status);
            userMapper.updateById(user);
        }
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    private UserProfileDTO convertToProfile(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ncu/chat/service/
git commit -m "feat: 添加 UserService 接口和实现"
```

---

## Task 8: AuthController

**Files:**
- Create: `backend/src/main/java/com/ncu/chat/controller/AuthController.java`

- [ ] **Step 1: 创建 AuthController**

```java
package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.model.dto.UserLoginDTO;
import com.ncu.chat.model.dto.UserRegisterDTO;
import com.ncu.chat.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody UserRegisterDTO dto) {
        Map<String, Object> result = userService.register(dto);
        return Result.success("注册成功", result);
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody UserLoginDTO dto) {
        Map<String, Object> result = userService.login(dto);
        return Result.success("登录成功", result);
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestAttribute("userId") Long userId) {
        userService.updateStatus(userId, 0);
        return Result.success("登出成功", null);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ncu/chat/controller/AuthController.java
git commit -m "feat: 添加 AuthController（注册、登录、登出）"
```

---

## Task 9: UserController 和 FileController

**Files:**
- Create: `backend/src/main/java/com/ncu/chat/controller/UserController.java`
- Create: `backend/src/main/java/com/ncu/chat/controller/FileController.java`
- Create: `backend/src/main/java/com/ncu/chat/util/FileUtil.java`

- [ ] **Step 1: 创建 UserController**

```java
package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.model.dto.UserProfileDTO;
import com.ncu.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<UserProfileDTO> updateProfile(@RequestAttribute("userId") Long userId,
                                                @RequestBody UserProfileDTO dto) {
        return Result.success(userService.updateProfile(userId, dto));
    }

    @PutMapping("/password")
    public Result<?> changePassword(@RequestAttribute("userId") Long userId,
                                    @RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.success("密码修改成功", null);
    }

    @PutMapping("/status")
    public Result<?> updateStatus(@RequestAttribute("userId") Long userId,
                                  @RequestBody Map<String, Integer> params) {
        userService.updateStatus(userId, params.get("status"));
        return Result.success("状态更新成功", null);
    }
}
```

- [ ] **Step 2: 创建 FileUtil**

```java
package com.ncu.chat.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class FileUtil {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.allowed-types}")
    private String allowedTypes;

    @Value("${file.upload.max-size}")
    private String maxSize;

    public String upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (!allowed.contains(extension)) {
            throw new RuntimeException("不支持的文件类型: " + extension);
        }

        String filename = UUID.randomUUID().toString() + "." + extension;
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        file.transferTo(new File(dir, filename));
        return "/uploads/" + filename;
    }
}
```

- [ ] **Step 3: 创建 FileController**

```java
package com.ncu.chat.controller;

import com.ncu.chat.common.Result;
import com.ncu.chat.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileUtil fileUtil;

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String url = fileUtil.upload(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success("上传成功", result);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ncu/chat/controller/ backend/src/main/java/com/ncu/chat/util/FileUtil.java
git commit -m "feat: 添加 UserController 和 FileController"
```

---

## Task 10: 数据库迁移脚本

**Files:**
- Create: `db/migration/user.sql`（A 负责）
- Create: `db/migration/friend_group.sql`（D 负责）
- Create: `db/migration/friendship.sql`（D 负责）
- Create: `db/migration/private_message.sql`（B 负责）
- Create: `db/migration/chat_group.sql`（C 负责）
- Create: `db/migration/group_member.sql`（C 负责）
- Create: `db/migration/group_message.sql`（C 负责）
- Create: `db/migration/voice_message.sql`（F 负责）
- Create: `db/migration/file_record.sql`（E 负责）
- Create: `db/migration/announcement.sql`（F 负责）
- Create: `db/migration/sensitive_word.sql`（F 负责）
- Create: `db/migrate.sh`
- Create: `db/init.sql`

- [ ] **Step 1: 创建 init.sql（建库）**

```sql
CREATE DATABASE IF NOT EXISTS chat_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

- [ ] **Step 2: A 创建 user.sql**

```sql
USE chat_system;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `signature` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT DEFAULT 0 COMMENT '0-离线 1-在线 2-忙碌 3-勿扰',
    `role` TINYINT DEFAULT 0 COMMENT '0-普通用户 1-管理员',
    `enabled` TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    `deleted` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `last_login_time` DATETIME DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 3: 创建 migrate.sh**

```bash
#!/bin/bash
set -e

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-root}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== 数据库迁移脚本 ==="

echo "1. 初始化数据库..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" < "$SCRIPT_DIR/init.sql"

echo "2. 执行迁移..."
for sql_file in "$SCRIPT_DIR/migration"/*.sql; do
    if [ -f "$sql_file" ]; then
        echo "   执行: $(basename "$sql_file")"
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" < "$sql_file"
    fi
done

echo "=== 迁移完成 ==="
```

- [ ] **Step 4: Commit**

```bash
mkdir -p db/migration
chmod +x db/migrate.sh
git add db/
git commit -m "feat: 添加数据库迁移脚本和 user 表（其他表由各成员添加）"
```

**注意：其他表的 SQL 文件由各成员自行创建并提交到 `db/migration/` 目录。**

---

## Task 11: 前端公共 CSS 和 JS

**Files:**
- Create: `backend/src/main/resources/static/css/style.css`
- Create: `backend/src/main/resources/static/js/api.js`
- Create: `backend/src/main/resources/static/js/auth.js`
- Create: `backend/src/main/resources/static/js/utils.js`
- Create: `backend/src/main/resources/static/js/websocket.js`

- [ ] **Step 1: 创建 style.css（基础样式）**

```css
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    background: #f0f2f5;
    color: #333;
}

.container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}

.btn {
    padding: 8px 16px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
}

.btn-primary {
    background: #1890ff;
    color: white;
}

.btn-primary:hover {
    background: #40a9ff;
}

.btn-danger {
    background: #ff4d4f;
    color: white;
}

.input-group {
    margin-bottom: 16px;
}

.input-group label {
    display: block;
    margin-bottom: 4px;
    font-weight: 500;
}

.input-group input {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    font-size: 14px;
}

.card {
    background: white;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* 主界面布局 */
.main-layout {
    display: flex;
    height: 100vh;
}

.sidebar {
    width: 300px;
    background: white;
    border-right: 1px solid #e8e8e8;
    display: flex;
    flex-direction: column;
}

.chat-area {
    flex: 1;
    display: flex;
    flex-direction: column;
}

.chat-header {
    padding: 16px;
    border-bottom: 1px solid #e8e8e8;
    background: white;
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
}

.chat-input {
    padding: 16px;
    border-top: 1px solid #e8e8e8;
    background: white;
}

/* 消息气泡 */
.message {
    display: flex;
    margin-bottom: 16px;
}

.message.sent {
    flex-direction: row-reverse;
}

.message .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    margin: 0 8px;
}

.message .content {
    max-width: 60%;
}

.message .bubble {
    padding: 8px 12px;
    border-radius: 8px;
    background: white;
    border: 1px solid #e8e8e8;
}

.message.sent .bubble {
    background: #1890ff;
    color: white;
    border: none;
}

.message .time {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
}

/* 未读角标 */
.badge {
    background: #ff4d4f;
    color: white;
    border-radius: 10px;
    padding: 2px 6px;
    font-size: 12px;
    min-width: 18px;
    text-align: center;
}
```

- [ ] **Step 2: 创建 api.js**

```javascript
const API = {
    baseURL: '',

    async request(url, options = {}) {
        const token = localStorage.getItem('token');
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const response = await fetch(this.baseURL + url, {
                ...options,
                headers
            });
            const data = await response.json();

            if (data.code === 401) {
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                window.location.href = '/login.html';
                return null;
            }

            return data;
        } catch (error) {
            console.error('API Error:', error);
            return { code: 500, message: '网络错误' };
        }
    },

    get(url) {
        return this.request(url, { method: 'GET' });
    },

    post(url, body) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    put(url, body) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    delete(url) {
        return this.request(url, { method: 'DELETE' });
    },

    async upload(file) {
        const token = localStorage.getItem('token');
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch('/api/file/upload', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });
        return response.json();
    }
};
```

- [ ] **Step 3: 创建 auth.js**

```javascript
const Auth = {
    isLoggedIn() {
        return !!localStorage.getItem('token');
    },

    getToken() {
        return localStorage.getItem('token');
    },

    getUser() {
        const user = localStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    },

    setUser(user) {
        localStorage.setItem('user', JSON.stringify(user));
    },

    setToken(token) {
        localStorage.setItem('token', token);
    },

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login.html';
    },

    checkAuth() {
        if (!this.isLoggedIn()) {
            window.location.href = '/login.html';
            return false;
        }
        return true;
    }
};
```

- [ ] **Step 4: 创建 utils.js**

```javascript
const Utils = {
    formatTime(dateStr) {
        const date = new Date(dateStr);
        const now = new Date();
        const diff = now - date;

        if (diff < 60000) return '刚刚';
        if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
        if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');

        if (year === now.getFullYear()) {
            return `${month}-${day} ${hours}:${minutes}`;
        }
        return `${year}-${month}-${day} ${hours}:${minutes}`;
    },

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },

    getAvatarUrl(avatar) {
        return avatar || '/assets/default-avatar.png';
    }
};
```

- [ ] **Step 5: 创建 websocket.js**

```javascript
const WS = {
    stompClient: null,
    connected: false,
    callbacks: {},

    connect(token) {
        const socket = new SockJS('/ws?token=' + token);
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null;

        this.stompClient.connect({}, (frame) => {
            this.connected = true;
            console.log('WebSocket connected');

            // 订阅个人消息
            this.stompClient.subscribe('/user/queue/messages', (message) => {
                const data = JSON.parse(message.body);
                this.trigger('message', data);
            });

            // 订阅通知
            this.stompClient.subscribe('/user/queue/notifications', (notification) => {
                const data = JSON.parse(notification.body);
                this.trigger('notification', data);
            });

            // 订阅在线状态
            this.stompClient.subscribe('/topic/status', (status) => {
                const data = JSON.parse(status.body);
                this.trigger('status', data);
            });

            this.trigger('connected');
        }, (error) => {
            console.error('WebSocket error:', error);
            this.connected = false;
            setTimeout(() => this.connect(token), 5000);
        });
    },

    subscribeGroup(groupId) {
        if (this.stompClient && this.connected) {
            this.stompClient.subscribe('/topic/group/' + groupId, (message) => {
                const data = JSON.parse(message.body);
                this.trigger('groupMessage', data);
            });
        }
    },

    send(destination, body) {
        if (this.stompClient && this.connected) {
            this.stompClient.send(destination, {}, JSON.stringify(body));
        }
    },

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
            this.connected = false;
        }
    },

    on(event, callback) {
        if (!this.callbacks[event]) {
            this.callbacks[event] = [];
        }
        this.callbacks[event].push(callback);
    },

    trigger(event, data) {
        if (this.callbacks[event]) {
            this.callbacks[event].forEach(cb => cb(data));
        }
    }
};
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/static/
git commit -m "feat: 添加前端公共 CSS 和 JS（api, auth, utils, websocket）"
```

---

## Task 12: 前端页面 — 登录和注册

**Files:**
- Create: `backend/src/main/resources/static/login.html`
- Create: `backend/src/main/resources/static/register.html`

- [ ] **Step 1: 创建 login.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>登录 - 在线聊天系统</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .login-container {
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }
        .login-card {
            width: 400px;
        }
        .login-card h2 {
            text-align: center;
            margin-bottom: 24px;
        }
        .error-msg {
            color: #ff4d4f;
            font-size: 14px;
            margin-bottom: 12px;
            display: none;
        }
        .register-link {
            text-align: center;
            margin-top: 16px;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <div class="card login-card">
            <h2>登录</h2>
            <div id="errorMsg" class="error-msg"></div>
            <form id="loginForm">
                <div class="input-group">
                    <label>用户名</label>
                    <input type="text" id="username" placeholder="请输入用户名" required>
                </div>
                <div class="input-group">
                    <label>密码</label>
                    <input type="password" id="password" placeholder="请输入密码" required>
                </div>
                <button type="submit" class="btn btn-primary" style="width:100%">登录</button>
            </form>
            <div class="register-link">
                没有账号？<a href="/register.html">立即注册</a>
            </div>
        </div>
    </div>
    <script src="/js/api.js"></script>
    <script src="/js/auth.js"></script>
    <script>
        document.getElementById('loginForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            const errorMsg = document.getElementById('errorMsg');

            const result = await API.post('/api/auth/login', { username, password });
            if (result.code === 200) {
                Auth.setToken(result.data.token);
                Auth.setUser(result.data.user);
                window.location.href = '/index.html';
            } else {
                errorMsg.textContent = result.message;
                errorMsg.style.display = 'block';
            }
        });
    </script>
</body>
</html>
```

- [ ] **Step 2: 创建 register.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>注册 - 在线聊天系统</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .register-container {
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }
        .register-card {
            width: 400px;
        }
        .register-card h2 {
            text-align: center;
            margin-bottom: 24px;
        }
        .error-msg {
            color: #ff4d4f;
            font-size: 14px;
            margin-bottom: 12px;
            display: none;
        }
        .login-link {
            text-align: center;
            margin-top: 16px;
        }
    </style>
</head>
<body>
    <div class="register-container">
        <div class="card register-card">
            <h2>注册</h2>
            <div id="errorMsg" class="error-msg"></div>
            <form id="registerForm">
                <div class="input-group">
                    <label>用户名</label>
                    <input type="text" id="username" placeholder="3-20个字符" required>
                </div>
                <div class="input-group">
                    <label>密码</label>
                    <input type="password" id="password" placeholder="至少6位，包含字母和数字" required>
                </div>
                <div class="input-group">
                    <label>确认密码</label>
                    <input type="password" id="confirmPassword" placeholder="再次输入密码" required>
                </div>
                <div class="input-group">
                    <label>昵称</label>
                    <input type="text" id="nickname" placeholder="请输入昵称" required>
                </div>
                <button type="submit" class="btn btn-primary" style="width:100%">注册</button>
            </form>
            <div class="login-link">
                已有账号？<a href="/login.html">立即登录</a>
            </div>
        </div>
    </div>
    <script src="/js/api.js"></script>
    <script src="/js/auth.js"></script>
    <script>
        document.getElementById('registerForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const nickname = document.getElementById('nickname').value;
            const errorMsg = document.getElementById('errorMsg');

            const result = await API.post('/api/auth/register', {
                username, password, confirmPassword, nickname
            });
            if (result.code === 200) {
                Auth.setToken(result.data.token);
                Auth.setUser(result.data.user);
                window.location.href = '/index.html';
            } else {
                errorMsg.textContent = result.message;
                errorMsg.style.display = 'block';
            }
        });
    </script>
</body>
</html>
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/static/login.html backend/src/main/resources/static/register.html
git commit -m "feat: 添加登录和注册页面"
```

---

## Task 13: 前端页面 — 主界面骨架和个人资料

**Files:**
- Create: `backend/src/main/resources/static/index.html`
- Create: `backend/src/main/resources/static/profile.html`

- [ ] **Step 1: 创建 index.html（主界面骨架）**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>在线聊天系统</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
    <div class="main-layout">
        <div class="sidebar">
            <div class="sidebar-header" style="padding:16px;border-bottom:1px solid #e8e8e8;">
                <div style="display:flex;align-items:center;justify-content:space-between;">
                    <span id="currentUser" style="font-weight:bold;"></span>
                    <div>
                        <select id="statusSelect" onchange="changeStatus(this.value)">
                            <option value="1">在线</option>
                            <option value="2">忙碌</option>
                            <option value="3">勿扰</option>
                            <option value="0">离线</option>
                        </select>
                    </div>
                </div>
            </div>
            <div class="sidebar-tabs" style="display:flex;border-bottom:1px solid #e8e8e8;">
                <div class="tab active" data-tab="recent" onclick="switchTab('recent')">最近</div>
                <div class="tab" data-tab="friends" onclick="switchTab('friends')">好友</div>
                <div class="tab" data-tab="groups" onclick="switchTab('groups')">群聊</div>
            </div>
            <div id="sidebarContent" class="sidebar-content" style="flex:1;overflow-y:auto;">
                <!-- 最近会话 / 好友列表 / 群列表 -->
            </div>
        </div>
        <div class="chat-area">
            <div class="chat-header">
                <span id="chatTitle">选择一个会话开始聊天</span>
            </div>
            <div class="chat-messages" id="chatMessages">
                <div style="text-align:center;color:#999;padding:40px;">
                    选择左侧会话开始聊天
                </div>
            </div>
            <div class="chat-input">
                <div style="display:flex;">
                    <input type="text" id="messageInput" placeholder="输入消息..." style="flex:1;padding:8px;border:1px solid #d9d9d9;border-radius:4px;" onkeypress="if(event.key==='Enter')sendMessage()">
                    <button class="btn btn-primary" onclick="sendMessage()" style="margin-left:8px;">发送</button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
    <script src="/js/api.js"></script>
    <script src="/js/auth.js"></script>
    <script src="/js/utils.js"></script>
    <script src="/js/websocket.js"></script>
    <script>
        if (!Auth.checkAuth()) location.href = '/login.html';

        const user = Auth.getUser();
        document.getElementById('currentUser').textContent = user.nickname;

        WS.connect(Auth.getToken());
        WS.on('message', (data) => {
            console.log('收到消息:', data);
            // TODO: 成员B实现消息渲染
        });

        async function changeStatus(status) {
            await API.put('/api/user/status', { status: parseInt(status) });
        }

        function switchTab(tab) {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            document.querySelector(`.tab[data-tab="${tab}"]`).classList.add('active');
            // TODO: 切换内容
        }

        function sendMessage() {
            const input = document.getElementById('messageInput');
            const content = input.value.trim();
            if (!content) return;
            // TODO: 成员B实现发送逻辑
            input.value = '';
        }
    </script>
    <style>
        .tab {
            flex: 1;
            text-align: center;
            padding: 12px;
            cursor: pointer;
            border-bottom: 2px solid transparent;
        }
        .tab.active {
            border-bottom-color: #1890ff;
            color: #1890ff;
        }
    </style>
</body>
</html>
```

- [ ] **Step 2: 创建 profile.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>个人资料 - 在线聊天系统</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .profile-container {
            max-width: 600px;
            margin: 40px auto;
        }
        .avatar-upload {
            text-align: center;
            margin-bottom: 20px;
        }
        .avatar-upload img {
            width: 100px;
            height: 100px;
            border-radius: 50%;
            cursor: pointer;
        }
        .success-msg {
            color: #52c41a;
            font-size: 14px;
            margin-bottom: 12px;
            display: none;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="profile-container">
            <div class="card">
                <h2 style="text-align:center;margin-bottom:24px;">个人资料</h2>
                <div id="successMsg" class="success-msg"></div>
                <div id="errorMsg" class="error-msg"></div>
                <div class="avatar-upload">
                    <img id="avatarPreview" src="/assets/default-avatar.png" onclick="document.getElementById('avatarInput').click()">
                    <input type="file" id="avatarInput" accept="image/*" style="display:none" onchange="uploadAvatar(this)">
                </div>
                <form id="profileForm">
                    <div class="input-group">
                        <label>用户名</label>
                        <input type="text" id="username" disabled>
                    </div>
                    <div class="input-group">
                        <label>昵称</label>
                        <input type="text" id="nickname">
                    </div>
                    <div class="input-group">
                        <label>个性签名</label>
                        <input type="text" id="signature">
                    </div>
                    <button type="submit" class="btn btn-primary" style="width:100%">保存修改</button>
                </form>
                <hr style="margin:24px 0;">
                <h3 style="margin-bottom:16px;">修改密码</h3>
                <form id="passwordForm">
                    <div class="input-group">
                        <label>原密码</label>
                        <input type="password" id="oldPassword" required>
                    </div>
                    <div class="input-group">
                        <label>新密码</label>
                        <input type="password" id="newPassword" required>
                    </div>
                    <button type="submit" class="btn btn-primary" style="width:100%">修改密码</button>
                </form>
                <div style="text-align:center;margin-top:16px;">
                    <a href="/index.html">返回聊天</a>
                </div>
            </div>
        </div>
    </div>
    <script src="/js/api.js"></script>
    <script src="/js/auth.js"></script>
    <script>
        if (!Auth.checkAuth()) location.href = '/login.html';

        let currentAvatar = '';

        async function loadProfile() {
            const result = await API.get('/api/user/profile');
            if (result.code === 200) {
                const user = result.data;
                document.getElementById('username').value = user.username;
                document.getElementById('nickname').value = user.nickname || '';
                document.getElementById('signature').value = user.signature || '';
                if (user.avatar) {
                    document.getElementById('avatarPreview').src = user.avatar;
                    currentAvatar = user.avatar;
                }
            }
        }

        async function uploadAvatar(input) {
            const file = input.files[0];
            if (!file) return;
            const result = await API.upload(file);
            if (result.code === 200) {
                currentAvatar = result.data.url;
                document.getElementById('avatarPreview').src = currentAvatar;
            }
        }

        document.getElementById('profileForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const result = await API.put('/api/user/profile', {
                nickname: document.getElementById('nickname').value,
                signature: document.getElementById('signature').value,
                avatar: currentAvatar
            });
            if (result.code === 200) {
                Auth.setUser(result.data);
                showSuccess('保存成功');
            }
        });

        document.getElementById('passwordForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const result = await API.put('/api/user/password', {
                oldPassword: document.getElementById('oldPassword').value,
                newPassword: document.getElementById('newPassword').value
            });
            if (result.code === 200) {
                showSuccess('密码修改成功');
                document.getElementById('oldPassword').value = '';
                document.getElementById('newPassword').value = '';
            } else {
                document.getElementById('errorMsg').textContent = result.message;
                document.getElementById('errorMsg').style.display = 'block';
            }
        });

        function showSuccess(msg) {
            const el = document.getElementById('successMsg');
            el.textContent = msg;
            el.style.display = 'block';
            setTimeout(() => el.style.display = 'none', 3000);
        }

        loadProfile();
    </script>
</body>
</html>
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/static/index.html backend/src/main/resources/static/profile.html
git commit -m "feat: 添加主界面骨架和个人资料页面"
```

---

## Task 14: 部署配置

**Files:**
- Create: `backend/Dockerfile`
- Create: `backend/docker-compose.yml`
- Create: `backend/deploy.sh`
- Create: `backend/.dockerignore`

- [ ] **Step 1: 创建 Dockerfile**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: 创建 docker-compose.yml**

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: chat-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: chat_system
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql
      - ./db/migration:/docker-entrypoint-initdb.d/migration
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  app:
    build: .
    container_name: chat-app
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/chat_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
    volumes:
      - uploads:/app/uploads
    depends_on:
      - mysql

volumes:
  mysql-data:
  uploads:
```

- [ ] **Step 3: 创建 deploy.sh**

```bash
#!/bin/bash
set -e

echo "=== 在线聊天系统部署脚本 ==="

echo "1. 构建并启动服务..."
docker-compose up -d --build

echo "2. 等待 MySQL 就绪..."
sleep 30

echo "3. 检查服务状态..."
docker-compose ps

echo ""
echo "=== 部署完成 ==="
echo "访问地址: http://localhost:8080"
echo "MySQL 地址: localhost:3306"
echo ""
echo "常用命令:"
echo "  查看日志: docker-compose logs -f"
echo "  停止服务: docker-compose down"
echo "  重启服务: docker-compose restart"
```

- [ ] **Step 4: 创建 .dockerignore**

```
target/
*.class
*.jar
*.war
.git
.gitignore
.idea
*.iml
.DS_Store
```

- [ ] **Step 5: Commit**

```bash
git add backend/Dockerfile backend/docker-compose.yml backend/deploy.sh backend/.dockerignore
chmod +x backend/deploy.sh
git commit -m "feat: 添加 Docker 部署配置和脚本"
```

---

## 完成检查

- [ ] 所有 API 接口可通过 Postman 测试
- [ ] 登录注册流程完整可用
- [ ] JWT 认证拦截器生效
- [ ] WebSocket 连接建立成功
- [ ] 前端页面可正常访问和交互
- [ ] `docker-compose up` 可一键启动
