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

    public String getTextFilingContentFromHTML(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return doc.text();
    }

//    public List<TenkFilingDbItem> getTenkFilingDbItemsFromRawFiling(String filingName, Map<String, String> processedFiling) {
//        List<TenkFilingDbItem> tenkFilingDbItems = new ArrayList<>();
//
//        for (Map.Entry<String, String> kvEntry : processedFiling.entrySet()) {
//            String itemName = kvEntry.getKey();
//            String htmlContent = kvEntry.getValue();
//            // set default value to "N/A" as null is not acceptable for hbaseDao
//            if (htmlContent == null || htmlContent.isEmpty()){
//                htmlContent = "N/A";
//            }
//
//            // get composed key first
//            String partNumber = getPartNumber(itemName);
//
//            // get text content from the existing html content
//            String textContent = getTextFilingContentFromHTML(htmlContent);
//
//            // build the filing item
//            TenkFilingDbItem filingItem = new TenkFilingDbItem();
//            filingItem.setFilingName(filingName);
//            filingItem.setPartNumber(partNumber);
//            filingItem.setItemNumber(itemName);
//            filingItem.setTextContent(textContent);
//            filingItem.setHtmlContent(htmlContent);
//            tenkFilingDbItems.add(filingItem);
//
//        }
//        return tenkFilingDbItems;
//    }

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
