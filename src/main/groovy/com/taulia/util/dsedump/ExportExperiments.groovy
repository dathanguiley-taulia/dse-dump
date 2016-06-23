package com.taulia.util.dsedump

import com.datastax.driver.core.*

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.security.*
import java.security.cert.CertificateException

/**
 * Hello world!
 *
 */
public class ExportExperiments {

  public static void main(String[] args) {
    /* cassandra {
          host = 'localhost'
          port = "${project.property('testEnvironment.cassandra.port')}"
          username = 'cassandra'
          password = 'cassandra'
          keyspaceName = 'Invoice'
          keystoreFile = rootProjectFileResolver.resolveCanonically('server/src/test/resources/certs/client/appserver.pfx')
          keystorePassphrase = 'password'
          keystoreType = 'PKCS12'
          truststoreFile = rootProjectFileResolver.resolveCanonically('server/src/test/resources/certs/server/serverstore.jks')
          truststorePassphrase = 'password'
        }*/
//    String host = "na1-qetest-datastax-node1-green.taulia.com"
    String host = "localhost"
    String username = "cassandra";
    String password = "cassandra";

    String truststorePath = "/Users/dathan.guiley/dev/intapi-invoice/server/src/test/resources/certs/server/serverstore.jks"
    String truststorePassword = "password"

    String keyspace = 'taulia_invoice'
    String table = "invoice"
    String buyerCompanyId = 'ff80808133603eb90133608bdd250056'

    Cluster cluster = Cluster.builder()
      .addContactPoints(host)
      .withPort(9142)
//      .withCredentials(username, password)
    //.withSSL(/*getSSLOptions(truststorePath, truststorePassword)*/)
      .build()

    Session session = cluster.connect();

    def base = System.currentTimeMillis()
    [buyerCompanyId, 'aaaaaaaaaaaaaaaaaaaaaaaaaa'].each { bcId ->
      (1..100).each {
        def id = base + bcId.hashCode() + it  //weird unique id
        session.execute("insert into taulia_invoice.invoice (id,buyer_company_id) values ('${id}','${bcId}');")
      }
    }
    println('populated')

    String query = "SELECT id,buyer_company_id FROM taulia_invoice.invoice;"
    Statement stmt = new SimpleStatement(query)
    stmt.setFetchSize(5000);
    ResultSet rs = session.execute(stmt);

    def count=0
    for (Row row : rs) {
      if (rs.getAvailableWithoutFetching() == 100 && !rs.isFullyFetched())  //if we have almost finished page
        rs.fetchMoreResults(); // this is asynchronous

      // Process the row ...
      def id = row.getString(0).toString()
      def buyer_company_id = row.getString(1).toString()
      if(buyer_company_id==buyerCompanyId)
      System.out.println(id+","+buyer_company_id+","+(count++));
    }

    session.close();
    cluster.close();

  }

  private static SSLOptions getSSLOptions(String truststorePath, String truststorePassword) {
    if (!truststorePath)
      throw new RuntimeException("truststorePath must both be specified.")

    try {
      SSLContext context = getSSLContext(truststorePath, truststorePassword);
      String[] css = SSLOptions.DEFAULT_SSL_CIPHER_SUITES;
      return new SSLOptions(context, css)
    }
    catch (UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static SSLContext getSSLContext(String truststorePath, String truststorePassword)
    throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {

    FileInputStream tsf = null;
    try {
      tsf = new FileInputStream(truststorePath);
      SSLContext ctx = SSLContext.getInstance("SSL");

      KeyStore ts = KeyStore.getInstance("JKS");
      ts.load(tsf, (truststorePassword ?: "").toCharArray());
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(ts);

      //FileInputStream ksf = null;
      /*  KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(ksf, keystorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keystorePassword.toCharArray());*/

      ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
      return ctx
    }
    finally {
      tsf.close();
    }
  }

}