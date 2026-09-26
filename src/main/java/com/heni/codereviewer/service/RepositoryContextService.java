package com.heni.codereviewer.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class RepositoryContextService {

    private final GitHubService gitHubService;

    private static final int MAX_CONTEXT_FILES = 5;

    private static final Pattern IMPORT_PATTERN =
            Pattern.compile(
                    "import\\s+(?:static\\s+)?([\\w.]+)(?:\\.\\*)?;"
            );

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile(
                    "package\\s+([\\w.]+);"
            );

    public RepositoryContextService(
            GitHubService gitHubService) {

        this.gitHubService = gitHubService;
    }

    public String buildContext(
            String owner,
            String repository,
            String changedFilePath,
            String changedFileContent,
            String commitSha) {

        StringBuilder context = new StringBuilder();

        context.append("=== CHANGED FILE ===\n");
        context.append("File: ")
                .append(changedFilePath)
                .append("\n\n");

        context.append(changedFileContent)
                .append("\n\n");

        List<String> repositoryFiles =
                gitHubService.getRepositoryFilePaths(
                        owner,
                        repository,
                        commitSha
                );

        Set<String> relatedFiles = findRelatedFiles(
                        changedFilePath,
                        changedFileContent,
                        repositoryFiles
                );

        context.append("=== RELATED REPOSITORY FILES ===\n");

        int filesRetrieved = 0;

        for (String filePath : relatedFiles) {

            if (filesRetrieved >= MAX_CONTEXT_FILES) {
                break;
            }

            try {

                String content =
                        gitHubService.getFileContent(
                                owner,
                                repository,
                                filePath,
                                commitSha
                        );

                if (!content.isBlank()) {

                    context.append("\n--- ")
                            .append(filePath)
                            .append(" ---\n");

                    context.append(content)
                            .append("\n");

                    filesRetrieved++;

                    System.out.println(
                            "Retrieved related file: "
                                    + filePath
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "Could not retrieve: "
                                + filePath
                );
            }
        }

        System.out.println(
                "Retrieved "
                        + filesRetrieved
                        + " related repository files"
        );

        return context.toString();
    }
    
    private Set<String> findRelatedFiles(
            String changedFilePath,
            String sourceCode,
            List<String> repositoryFiles) {

        Set<String> relatedFiles =
                new LinkedHashSet<>();

        // Explicit imports
        Set<String> importedClasses =
                extractImports(sourceCode);

        for (String importedClass : importedClasses) {

            String className =
                    getClassName(importedClass);

            String matchingPath =
                    findMatchingJavaFile(
                            repositoryFiles,
                            className
                    );

            if (matchingPath != null
                    && !matchingPath.equals(changedFilePath)) {

                relatedFiles.add(matchingPath);
            }
        }

        // Same-package classes
        String packageName =
                extractPackage(sourceCode);

        if (packageName != null) {

            String packagePath =
                    packageName.replace(".", "/");

            for (String path : repositoryFiles) {

                if (!path.endsWith(".java")) {
                    continue;
                }

                if (path.equals(changedFilePath)) {
                    continue;
                }

                if (path.contains(
                        "/" + packagePath + "/"
                )) {

                    relatedFiles.add(path);
                }
            }
        }

        return relatedFiles;
    }

    private Set<String> extractImports(
            String sourceCode) {

        Set<String> imports =
                new LinkedHashSet<>();

        Matcher matcher =
                IMPORT_PATTERN.matcher(sourceCode);

        while (matcher.find()) {

            String importedClass =
                    matcher.group(1);

            if (isExternalDependency(importedClass)) {
                continue;
            }

            imports.add(importedClass);
        }

        return imports;
    }

    private String extractPackage(
            String sourceCode) {

        Matcher matcher =
                PACKAGE_PATTERN.matcher(sourceCode);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private boolean isExternalDependency(
            String importedClass) {

        return importedClass.startsWith("java.")
                || importedClass.startsWith("javax.")
                || importedClass.startsWith("jakarta.")
                || importedClass.startsWith("org.springframework.")
                || importedClass.startsWith("com.fasterxml.")
                || importedClass.startsWith("org.junit.")
                || importedClass.startsWith("org.mockito.");
    }

    private String getClassName(
            String importedClass) {

        int lastDot =
                importedClass.lastIndexOf('.');

        if (lastDot == -1) {
            return importedClass;
        }

        return importedClass.substring(
                lastDot + 1
        );
    }

    private String findMatchingJavaFile(
            List<String> repositoryFiles,
            String className) {

        String expectedFileName =
                className + ".java";

        for (String path : repositoryFiles) {

            if (path.endsWith(
                    "/" + expectedFileName
            )) {
                return path;
            }
        }

        for (String path : repositoryFiles) {

            if (path.equals(expectedFileName)) {
                return path;
            }
        }

        return null;
    }
}