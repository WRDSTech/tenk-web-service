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

    public static String getDecompressedBody(ResponseEntity<byte[]> formTxt) throws IOException {
        byte[] rawFilingContent = formTxt.getBody();
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(rawFilingContent));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = gzipInputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
