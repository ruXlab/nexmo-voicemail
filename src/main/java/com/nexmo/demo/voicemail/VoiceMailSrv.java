package com.nexmo.demo.voicemail;

import com.nexmo.client.NexmoClient;
import com.nexmo.client.auth.JWTAuthMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;


@SpringBootApplication
public class VoiceMailSrv {
    private static Logger log = LoggerFactory.getLogger(VoiceMailSrv.class);

    public static void main(String[] args) {
        SpringApplication.run(VoiceMailSrv.class, args);
    }

    @Bean
    public NexmoClient nexmoClient(
            @Value("${nexmoApplicationId}") String applicationId,
            @Value("classpath:nexmo_app.key") Resource cert
    ) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        return new NexmoClient(new JWTAuthMethod(applicationId, Files.readAllBytes(cert.getFile().toPath())));
    }
}
