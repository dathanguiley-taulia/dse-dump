package com.taulia.util.dsedump

import com.datastax.driver.core.*

public class Export {

  public static void main(String[] args) {
    Cluster cluster = buildClusterFromArgs(args)

    String buyerCompanyId = args[0]
    System.err.println("""Collecting invoiceIds for buyerCompanyId:${buyerCompanyId}""")

    Session session = cluster.connect();
    String query = "SELECT id,buyer_company_id FROM taulia_invoice.invoice;"
    Statement stmt = new SimpleStatement(query)
    stmt.setFetchSize(5000);
    ResultSet rs = session.execute(stmt);

    def count = 0
    for (Row row : rs) {
      if (rs.getAvailableWithoutFetching() == 200 && !rs.isFullyFetched())  //if we have almost finished page
        rs.fetchMoreResults(); // this is asynchronous

      // Process the row ...
      def id = row.getString(0).toString()
      def buyer_company_id = row.getString(1).toString()
      if (buyer_company_id != buyerCompanyId)
        continue

      System.out.println(id);

      count++
      if (count % 10000 == 0) {
        System.err.println(count)
      }
    }
    System.err.println(count)

    session.close();
    cluster.close();
  }

  private static Cluster buildClusterFromArgs(String[] args) {
    if (!args) {
      System.out.println('''
Usage:$ dse-dump buyer_company_id [host[:port] [username [password]]]
For ssl specify jvm opts with either DSE_DUMP_OPTS (or JAVA_OPTS) like this:
export DSE_DUMP_OPTS='-Djavax.net.ssl.trustStore=/Users/buck.rodgers/dev/intapi-invoice/server/src/test/resources/certs/server/serverstore.jks -Djavax.net.ssl.trustStorePassword=password'
''')
      System.exit(1)
    }

    String host = args.size() > 1 ? args[1] : "localhost"
    String username = args.size() > 2 ? args[2] : "";
    String password = args.size() > 3 ? args[3] : "";
    int port
    boolean ssl

    def matcher = host =~ /(.*):(.*)/
    if (matcher) {
      port = Integer.valueOf(matcher[0][2])
      host = matcher[0][1]
    } else {
      port = 9042
    }

    System.err.println("""Connecting to host:${host} port:${port} ssl:${ssl} username:${username} password:${
      password ? 'Y' : 'N'
    } trustStore:${System.getProperty('javax.net.ssl.trustStore')}""")

    def builder = Cluster.builder()
      .addContactPoints(host)
      .withPort(port)
    if (username) {
      builder = builder.withCredentials(username, password)
    }
    if (System.getProperty('javax.net.ssl.trustStore')) {
      builder = builder.withSSL()
    }
     builder.build()
  }
}