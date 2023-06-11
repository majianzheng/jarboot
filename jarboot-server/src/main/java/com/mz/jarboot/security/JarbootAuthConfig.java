package com.mz.jarboot.security;

import com.mz.jarboot.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;


/**
 * @author majianzheng
 */
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class JarbootAuthConfig extends WebSecurityConfigurerAdapter {

    private static final String SECURITY_IGNORE_URLS_SPILT_CHAR = ",";

    private static final String LOGIN_ENTRY_POINT = "/api/jarboot/auth/login";

    private static final String TOKEN_BASED_AUTH_ENTRY_POINT = "/api/jarboot/auth/**";
    
    private static final String DEFAULT_ALL_PATH_PATTERN = "/,/error,/**/*.css,/**/*.js,/**/*.html,/**/*.map,/**/*.svg,/**/*.png,/**/*.woff2,/**/*.ico,/actuator/**";
    
    private static final String PROPERTY_IGNORE_URLS = "jarboot.security.ignore.urls";

    @Autowired
    private Environment env;
    
    @Autowired
    private JwtTokenManager tokenProvider;
    
    @Autowired
    private JarbootUserDetailsServiceImpl userDetailsService;
    
    @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
    
    @Override
    public void configure(WebSecurity web) {
        String ignoreUrls = env.getProperty(PROPERTY_IGNORE_URLS, DEFAULT_ALL_PATH_PATTERN);
        if (!StringUtils.isBlank(ignoreUrls)) {
            for (String each : ignoreUrls.trim().split(SECURITY_IGNORE_URLS_SPILT_CHAR)) {
                web.ignoring().antMatchers(each.trim());
            }
        }
        web.ignoring().antMatchers(
                TOKEN_BASED_AUTH_ENTRY_POINT,
                "/**/public/**",
                "/api/jarboot/cloud/**");
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().cors()// We don't need CSRF for JWT based authentication
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().authorizeRequests().requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .antMatchers(LOGIN_ENTRY_POINT).permitAll()
                .and().authorizeRequests().antMatchers(TOKEN_BASED_AUTH_ENTRY_POINT).authenticated()
                .and().exceptionHandling().authenticationEntryPoint(new JwtAuthenticationEntryPoint());
        // disable cache
        http.headers().cacheControl();

        http.addFilterBefore(new JwtAuthenticationTokenFilter(tokenProvider),
                UsernamePasswordAuthenticationFilter.class);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
}
