# Tenk Web Service 2.0

## Design Doc
https://docs.google.com/document/d/1uMvvdbmQBgrCU-Mb_kGwEfwxqtGvYfy5xToezgQzkDc/edit#

## Enviroment Setup
Setup the following environment before running the app.

### HBase docker
Setting up the hbase-docker by first `cd` into the `testing-storage-setup` directory, then execute `start-hbase.sh`

### Ozone docker cluster
Setting up the ozone docker cluster by first `cd` into the `testing-storage-setup` directory, then execute `start-ozone-cluster.sh`

### Remote Itemization Service
Haven't set up yet, most likely you'd be able to access `web-one:18000` to get this soon. 
It responses a json stream. Each response entry represents a filing item.