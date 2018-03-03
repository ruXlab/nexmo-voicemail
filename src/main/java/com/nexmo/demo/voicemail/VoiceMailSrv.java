package com.nexmo.demo.voicemail;

import com.nexmo.client.NexmoClient;
import com.nexmo.client.NexmoClientException;
import com.nexmo.client.auth.JWTAuthMethod;
import com.nexmo.client.sms.messages.TextMessage;
import com.nexmo.client.voice.Call;
import com.nexmo.client.voice.CallEvent;
import com.nexmo.client.voice.Recording;
import com.nexmo.client.voice.ncco.RecordNcco;
import com.nexmo.client.voice.ncco.StreamNcco;
import com.nexmo.client.voice.ncco.TalkNcco;
import com.nexmo.client.voice.servlet.NccoResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;

import static java.util.Arrays.asList;


@RestController
@SpringBootApplication
public class VoiceMailSrv {
    private static Logger log = LoggerFactory.getLogger(VoiceMailSrv.class);
    private static String destinationEmail = "ruslan.zaharov@vonage.com";

    @Autowired
    public JavaMailSender emailSender;

    @Autowired
    public NexmoClient nexmoClient;


    @GetMapping("/record")
    public void onRecordingFinished(
            @RequestParam Map<String, String> query,
            @RequestParam("recording_url") String recordingUrl
    ) throws MessagingException, IOException, NexmoClientException {

        Recording recording = nexmoClient.getVoiceClient().downloadRecording(recordingUrl);

        log.warn("onRecordingFinished: new recording detected: {}", recordingUrl);
        MimeMessage message = emailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(destinationEmail, "Voicemail demo service");
        helper.setSubject("Your voicemail on " + new Date());
        helper.addTo(destinationEmail);
        helper.setText("Please find voice recording in attachment");

        InputStream inputStream = new ByteArrayInputStream(query.toString().getBytes("UTF-8"));
        helper.addAttachment("Invoice", () -> recording.getContent(), "audio/mpeg3");

        emailSender.send(message);

    }

    @PostMapping("/event")
    public void onEvent(@RequestBody CallEvent callEvent) {
        System.out.println("event" + callEvent);
//        log.warn("booo {}", callEvent);
    }

    @GetMapping("/answer")
    public Object onAnswer(@RequestParam Map<String, String> query,
                           @RequestParam("conversation_uuid") String conversation,
                           @RequestParam("from") String caller,
                           HttpServletRequest request) {
        log.info("onAnswer: got call from {} (conversation {})", caller, conversation);
        return new NccoResponseBuilder()
            .appendNcco(new TalkNcco("Thanks for calling us. Please leave your message and press hash key"))
            .appendNcco(new SNcco("http://www.thesoundarchive.com/starwars/swvader03.mp3"))
            .appendNcco(new RecordNcco() {{
                String schema = Optional.ofNullable(request.getHeader("x-forwarded-proto")).orElse(request.getScheme());
                String host = Optional.ofNullable(request.getHeader("host")).orElse(request.getLocalAddr());
                setBeepStart(true);
                setEndOnKey('#');
                setEventUrl(String.format("%s://%s/record", schema, host));
                setEventMethod("GET");
            }})
            .appendNcco(new TalkNcco("Thank you. Your message has been recorded. Bye"))
            .getValue().toJson();
    }

    public static void main(String[] args) {
        SpringApplication.run(VoiceMailSrv.class, args);
    }

    @Bean
    public NexmoClient getNexmoClient(
            @Value("${nexmoApplicationId}") String applicationId,
            @Value("classpath:nexmo_app.key") Resource cert
    ) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, NexmoClientException {
        System.out.println("> " + applicationId + " > " + cert.getFile().length());
        String s = new NexmoClient(new JWTAuthMethod(applicationId, Files.readAllBytes(cert.getFile().toPath())))
                .generateJwt();
        System.out.println(">  " + s);
        return new NexmoClient(new JWTAuthMethod(applicationId, Files.readAllBytes(cert.getFile().toPath())));
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(25);

//        mailSender.setUsername("my.gmail@gmail.com");
//        mailSender.setPassword("password");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }
}
