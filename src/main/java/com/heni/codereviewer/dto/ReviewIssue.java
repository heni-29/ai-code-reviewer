package com.heni.codereviewer.dto;

public class ReviewIssue {

    public String category;
    public String severity;
    public String file;
    public int line;
    public String message;
    public String suggestion;
    public double confidence;

    @Override
    public String toString() {
        return "ReviewIssue{" +
                "category='" + category + '\'' +
                ", severity='" + severity + '\'' +
                ", file='" + file + '\'' +
                ", line=" + line +
                ", message='" + message + '\'' +
                ", suggestion='" + suggestion + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}