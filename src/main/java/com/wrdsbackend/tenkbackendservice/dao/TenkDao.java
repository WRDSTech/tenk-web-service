package com.wrdsbackend.tenkbackendservice.dao;

import com.flipkart.hbaseobjectmapper.AbstractHBDAO;
import com.wrdsbackend.tenkbackendservice.dbentity.TenkFilingDbItem;
import org.apache.hadoop.hbase.client.Connection;
import org.springframework.stereotype.Repository;

@Repository
public class TenkDao extends AbstractHBDAO<String, TenkFilingDbItem> {
    protected TenkDao(Connection connection) {
        super(connection);
    }
}
