package com.heni.codereviewer.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "review_issues")
public class ReviewIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;

    private String severity;

    private String file;

    private int line;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    private double confidence;

    private String repository;

    private int pullRequestNumber;

    public ReviewIssueEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public int getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(int pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    @Override
    public String toString() {
        return "ReviewIssueEntity{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", severity='" + severity + '\'' +
                ", file='" + file + '\'' +
                ", line=" + line +
                ", message='" + message + '\'' +
                ", suggestion='" + suggestion + '\'' +
                ", confidence=" + confidence +
                ", repository='" + repository + '\'' +
                ", pullRequestNumber=" + pullRequestNumber +
                '}';
    }
}