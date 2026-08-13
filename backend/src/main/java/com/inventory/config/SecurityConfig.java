package com.inventory.config;

import com.inventory.security.CustomUserDetailsService;
import com.inventory.security.JwtAuthenticationFilter;
import com.inventory.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // Public: login, department self-registration (goes through a pending-approval
                    // workflow with no client-controlled role), and barcode label images (embedded
                    // in <img> tags without an Authorization header).
                .antMatchers("/api/auth/login", "/api/auth/department/register", "/api/barcode/image/**").permitAll()
                // FIX: Allow public GET access to categories and departments used in frontend
                // dropdowns. This permits unauthenticated GET requests for these resources,
                // while write operations remain protected by method-level security
                // (e.g. @PreAuthorize("hasRole('ADMIN')") on controller methods).
                .antMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/departments").permitAll()
                // /api/auth/register accepts an explicit Role from the client, so it must only
                // ever be reachable by a caller already authenticated as ADMIN. Previously this
                // was folded into a blanket "/api/auth/**".permitAll(), which let anyone self-register
                // as ADMIN - that hole is closed here.
                .antMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/department/**").hasRole("DEPARTMENT")
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
                .and()
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
