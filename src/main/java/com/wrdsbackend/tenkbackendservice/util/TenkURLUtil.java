package com.wrdsbackend.tenkbackendservice.util;

import lombok.experimental.UtilityClass;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

@UtilityClass
public class TenkURLUtil {
    public static String extractFilingNameFromFilingTxtURL(String url) {

        String[] fragments = url.split("/");
        String filingNameWithExtension = fragments[fragments.length - 1];
        int endIdx = filingNameWithExtension.length();
        if (filingNameWithExtension.contains(".")) {
            endIdx = filingNameWithExtension.indexOf('.');
        }
        return filingNameWithExtension.substring(0, endIdx);
    }
}
