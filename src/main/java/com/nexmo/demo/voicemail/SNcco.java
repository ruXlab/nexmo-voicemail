package com.nexmo.demo.voicemail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nexmo.client.voice.ncco.Ncco;
import com.nexmo.client.voice.ncco.NccoSerializer;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SNcco implements Ncco {
    private static final String ACTION = "stream";

    private List<String> streamUrl;
    private Float level = null;
    private Boolean bargeIn = null;
    private Integer loop = null;

    public SNcco(@JsonProperty("streamUrl") String streamUrl) {
        this.streamUrl = asList(streamUrl);
    }

    public List<String> getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = Collections.singletonList(streamUrl);
    }

    public Float getLevel() {
        return level;
    }

    public void setLevel(Float level) {
        this.level = level;
    }

    public Boolean getBargeIn() {
        return bargeIn;
    }

    public void setBargeIn(Boolean bargeIn) {
        this.bargeIn = bargeIn;
    }

    public Integer getLoop() {
        return loop;
    }

    public void setLoop(Integer loop) {
        this.loop = loop;
    }

    @Override
    public String getAction() {
        return ACTION;
    }

    @Override
    public String toJson() {
        return NccoSerializer.getInstance().serializeNcco(this);
    }
}