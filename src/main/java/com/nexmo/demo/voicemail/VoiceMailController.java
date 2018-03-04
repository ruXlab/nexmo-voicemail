package com.nexmo.demo.voicemail;

import com.nexmo.client.NexmoClient;
import com.nexmo.client.NexmoClientException;
import com.nexmo.client.voice.Recording;
import com.nexmo.client.voice.ncco.RecordNcco;
import com.nexmo.client.voice.ncco.TalkNcco;
import com.nexmo.client.voice.servlet.NccoResponseBuilder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@RestController
public class VoiceMailController {
    private static Logger log = LoggerFactory.getLogger(VoiceMailController.class);

    @Value("${destinationEmail}")
    public String destinationEmail;

    @Value("${senderEmail}")
    public String senderEmail;

    @Autowired
    public JavaMailSender emailSender;

    @Autowired
    public NexmoClient nexmoClient;

    @GetMapping("/record")
    public void onRecordingFinished(
            @RequestParam("recording_url") String recordingUrl
    ) throws MessagingException, IOException, NexmoClientException {
        log.info("onRecordingFinished: new recording detected: {}", recordingUrl);

        Recording recording = nexmoClient.getVoiceClient().downloadRecording(recordingUrl);
        byte[] binaryRecording = IOUtils.toByteArray(recording.getContent());
        log.info("onRecordingFinished: recording has been downloaded, {}bytes", binaryRecording.length);

        MimeMessage message = emailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(senderEmail, "Voicemail demo service");
        helper.setSubject("Your voicemail recorded at " + new Date());
        helper.addTo(destinationEmail);
        helper.setText("Please find voice recording in attachment");

        helper.addAttachment("Recording.mp3", () -> new ByteArrayInputStream(binaryRecording), "audio/mpeg3");

        emailSender.send(message);
        log.info("onRecordingFinished: email has been sent to {}", destinationEmail);
    }

    @GetMapping("/event")
    public void onEvent(@RequestParam Map<String, String> callEvent) {
        log.warn("onEvent: {}", callEvent);
    }

    @GetMapping("/answer")
    public Object onAnswer(
           @RequestParam("conversation_uuid") String conversation,
           @RequestParam("from") String caller,
           HttpServletRequest request
    ) {
        log.info("onAnswer: got call from {} (conversation {})", caller, conversation);
        return new NccoResponseBuilder()
            .appendNcco(new TalkNcco("Thanks for calling us. Please leave your message and press hash key"))
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

}
