# dse-dump
A utility app to read the contents of a table in DSE and write out filtered data.

This is needed because many large data pulls will timeout in cassandra or solr.

*This app is currently hardcoded to pull invoice ids by buyer_company_id.  
However, the vision is to provide more filtering and output options.*

## Usage

1. unzip the dist
2. if using ssl export jvm opts for the trustStore info
3. call the app piping output to a file.
                       
You will see status in stderr and the results will pipe into out.txt

```
Usage:$ dse-dump buyer_company_id [host[:port] [username [password]]]
```

For ssl specify jvm opts with either DSE_DUMP_OPTS (or JAVA_OPTS) like this:

```
export DSE_DUMP_OPTS='-Djavax.net.ssl.trustStore=/Users/buck.rodgers/dev/intapi-invoice/server/src/test/resources/certs/server/serverstore.jks -Djavax.net.ssl.trustStorePassword=password'
```

### Examples

`$ export DSE_DUMP_OPTS='-Djavax.net.ssl.trustStore=/Users/buck.rodgers/dev/intapi-invoice/server/src/test/resources/certs/server/serverstore.jks -Djavax.net.ssl.trustStorePassword=password'`

`$ dse-dump ff80808133603eb90133608bdd250056 na1-qetest-datastax-node1-green.taulia.com:9042 cassandra cassandra > out.txt`
