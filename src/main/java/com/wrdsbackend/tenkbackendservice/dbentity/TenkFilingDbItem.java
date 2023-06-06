package com.wrdsbackend.tenkbackendservice.dbentity;

import com.flipkart.hbaseobjectmapper.Family;
import com.flipkart.hbaseobjectmapper.HBColumn;
import com.flipkart.hbaseobjectmapper.HBRecord;
import com.flipkart.hbaseobjectmapper.HBTable;
import lombok.Data;
import lombok.NoArgsConstructor;

@HBTable(name = "tenk-filing",
        families = {
                @Family(name = "content"),
        }
)
@Data
@NoArgsConstructor
public class TenkFilingDbItem implements HBRecord<String> {

    private String filingName;
    private String partNumber;
    private String itemNumber;

    @HBColumn(family = "content", column = "text")
    private String textContent;
    @HBColumn(family = "content", column = "html")
    private String htmlContent;

    @Override
    public String composeRowKey() {
        return String.format("%s#%s#%s", filingName, partNumber, itemNumber);
    }

    @Override
    public void parseRowKey(String rowKey) {
        String[] pieces = rowKey.split("#");
        this.filingName = pieces[0];
        this.partNumber = pieces[1];
        this.itemNumber = pieces[2];
    }
}
