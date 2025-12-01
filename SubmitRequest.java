package com.example.bh.dto;

public class SubmitRequest {
    private String finalQuery;

    public SubmitRequest() {}

    public SubmitRequest(String finalQuery) {
        this.finalQuery = finalQuery;
    }

    public String getFinalQuery() { return finalQuery; }
    public void setFinalQuery(String finalQuery) { this.finalQuery = finalQuery; }
}
