package com.factorenergia.core.pdfFiller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class HttpRequest {
    @JsonProperty("template")
    private String template;

    @JsonProperty("data")
    private List<HttpItemRequest> data;

    @JsonCreator
    public HttpRequest(@JsonProperty("template") String template, @JsonProperty("data") List<HttpItemRequest> data) {
        this.template = template;
        this.data = data;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public List<HttpItemRequest> getData() {
        return data;
    }

    public void setData(List<HttpItemRequest> data) {
        this.data = data;
    }
}
