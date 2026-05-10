package com.knowledge.enums;

import lombok.Getter;

@Getter
public enum FileType {

    TXT("txt", "text/plain"),
    MD("md", "text/markdown"),
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    DOC("doc", "application/msword");

    private final String extension;
    private final String mimeType;

    FileType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public static FileType fromExtension(String extension) {
        String ext = extension.toLowerCase().replace(".", "");
        for (FileType type : values()) {
            if (type.extension.equals(ext)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isSupported(String filename) {
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        for (FileType type : values()) {
            if (type.extension.equals(ext)) {
                return true;
            }
        }
        return false;
    }
}
