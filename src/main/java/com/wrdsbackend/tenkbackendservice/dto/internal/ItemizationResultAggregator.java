package com.wrdsbackend.tenkbackendservice.dto.internal;

import com.wrdsbackend.tenkbackendservice.dbentity.TenkFilingDbItem;

import java.util.List;
import java.util.zip.ZipOutputStream;


public record ItemizationResultAggregator (
        ZipOutputStream htmlOutputStream,
        ZipOutputStream textOutputStream,
        List<TenkFilingDbItem> tenkFilingDbItems
) {
}
