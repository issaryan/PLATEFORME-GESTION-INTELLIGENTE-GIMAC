package cm.gimac.des.iam.config;

import cm.gimac.des.iam.application.usecase.sandbox.ProprietesSandbox;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Paramètres de la Sandbox GIMAC dans MS-IAM (D-119, bloc gimac.sandbox). */
@Configuration
@EnableConfigurationProperties(ProprietesSandbox.class)
public class SandboxConfiguration {}
