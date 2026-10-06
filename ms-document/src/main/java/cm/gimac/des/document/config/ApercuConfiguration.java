package cm.gimac.des.document.config;

import cm.gimac.des.document.application.usecase.ApercuProprietes;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Aperçu des documents bureautiques (D-83) : réglages {@code gimac.apercu.*}. */
@Configuration
@EnableConfigurationProperties(ApercuProprietes.class)
public class ApercuConfiguration {}
