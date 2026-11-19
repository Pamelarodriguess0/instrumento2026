package com.curso.instrumento2026.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProtecaoAutenticacaoProperties.class)
public class ProtecaoAutenticacaoConfig { }
