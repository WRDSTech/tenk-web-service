package com.wrdsbackend.tenkbackendservice.config;

import lombok.Data;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Data
public class HBaseConfig {

    @Value("${hbase.zookeeper.quorum}")
    private String zkQuorum;
    @Value("${hbase.zookeeper.property.clientPort}")
    private String zkPort;
    @Bean
    public Connection hbaseConnection() throws IOException {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", zkQuorum);
        config.set("hbase.zookeeper.property.clientPort", zkPort);
        return ConnectionFactory.createConnection(config);
    }
}
