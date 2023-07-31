package com.wrdsbackend.tenkbackendservice.util;

import com.wrdsbackend.tenkbackendservice.config.TenkAppConfig;
import com.wrdsbackend.tenkbackendservice.dto.internal.FilingItemInternalRespDto;
import com.wrdsbackend.tenkbackendservice.dbentity.TenkFilingDbItem;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenkFilingDataUtil {

    private final TenkAppConfig tenkAppConfig;

    public String getPartNumber(String itemName) {
        return tenkAppConfig.getFilingItemToPartNumber().getOrDefault(itemName, "partx");
    }

    public TenkFilingDbItem convertTenkFilingDtoToTenkFilingDbItem(String filingName, FilingItemInternalRespDto filingItemInternalRespDto) {
        String itemName = filingItemInternalRespDto.getItemName();
        String htmlContent = filingItemInternalRespDto.getItemHtmlContent();
        String textContent = filingItemInternalRespDto.getItemTextContent();
        // set default value to "N/A" as null is not acceptable for hbaseDao
        if (htmlContent == null || htmlContent.isEmpty()){
            htmlContent = "N/A";
        }

        // get composed key first
        String partNumber = getPartNumber(itemName);

        // build the filing item
        TenkFilingDbItem filingDbItem = new TenkFilingDbItem();
        filingDbItem.setFilingName(filingName);
        filingDbItem.setPartNumber(partNumber);
        filingDbItem.setItemNumber(itemName);
        filingDbItem.setTextContent(textContent);
        filingDbItem.setHtmlContent(htmlContent);
        return filingDbItem;
    }
}
