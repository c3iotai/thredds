/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.net;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.util.CommonTestUtils;
import ucar.unidata.test.util.NotJenkins;
import ucar.unidata.test.util.NotTravis;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.io.Serializable;

/**
 * This test is to check ssh authorization.
 * As a rule, this needs to run against localhost:8443
 * using a pure tomcat server.
 * It currently cannot be run except manually under Intellij.
In order to properly set up your local tomcat server to
run these tests, you should follow the instructions in ?
Plus the following notes.
Notes:
1. Before running testMutualSSH(), you should read
   docs/website/netcdf-java/reference/ssh.adoc.
2. Since the certificates required for testing have a finite lifetime,
   you will need to regenerate a set of certificates (using certs.sh)
   and keystores before testing. Use ClientKeystore.jks as the argument
   to -Dkeystore and place ServerKeystore.jks and ServerTruststore.jks
   in the tomcat conf directory. The password is always "password".
3. In order to properly run the testMutualSSH() test, you will need to
   do the following:
   a. Change clientAuth="want" in server.xml to clientAuth="true"/
      The value want indicates that client-side
      certificate checking should be attempted, but it is ok if it fails.
      To force use of a clientside certificate, you need to use
      clientAuth="true". Note that this will cause testSSH() to fail.
   b. You need to pass in the location of the clientside keystore
      plus its password by adding the following to the jvm flags:
          -Dkeystore=<absolute path to ClientKeystore.jks>
	  -Dkeystorepassword=password
 */

@Category({NotJenkins.class, NotTravis.class})
public class TestSSH extends CommonTestUtils
{
    static protected final boolean IGNORE = true;

    //static protected String SERVER = "localhost:8443";
    static protected String SERVER = TestDir.remoteTestServer;

    static protected String Dkeystore = null;
    static protected String Dkeystorepassword = null;

    static final String[] sshurls = {
            "https://" + SERVER + "/thredds/dodsC/localContent/testData.nc.dds"
    };

    static {
        // Get the keystore properties
        Dkeystore = System.getProperty("keystore");
        Dkeystorepassword = System.getProperty("keystorepassword");
        // Set testing output
        HTTPSession.TESTING = true;
        HTTPMethod.TESTING = true;
    }

    //////////////////////////////////////////////////
    // Provide a non-interactive CredentialsProvider to hold
    // the user+pwd; used in several places

    static class TestProvider implements CredentialsProvider, Serializable
    {
        String username = null;
        String password = null;

        public TestProvider()
        {
        }

        public void setPWD(String username, String password)
        {
            this.username = username;
            this.password = password;
        }

        // Credentials Provider Interface
        public Credentials
        getCredentials(AuthScope scope) //AuthScheme authscheme, String host, int port, boolean isproxy)
        {
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
            System.err.printf("TestCredentials.getCredentials called: creds=|%s| host=%s port=%d%n",
                    creds.toString(), scope.getHost(), scope.getPort());
            return creds;
        }

        public void setCredentials(AuthScope scope, Credentials creds)
        {
            throw new UnsupportedOperationException();
        }

        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        // Serializable Interface
        private void writeObject(java.io.ObjectOutputStream oos)
                throws IOException
        {
            oos.writeObject(this.username);
            oos.writeObject(this.password);
        }

        private void readObject(java.io.ObjectInputStream ois)
                throws IOException, ClassNotFoundException
        {
            this.username = (String) ois.readObject();
            this.password = (String) ois.readObject();
        }
    }

    //////////////////////////////////////////////////

    // Define the test sets

    protected String datadir = null;
    protected String threddsroot = null;

    protected Result result = new Result();

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestSSH()
    {
        super("TestSSH");
        setTitle("SSH Authorization tests");
        //HTTPSession.debugHeaders(true);
    }

    @Test
    public void
    testSSH() throws Exception
    {
        String version = System.getProperty("java.version");
        Assume.assumeTrue("Version must be 1.8 (temporary), not: " + version,
                version.startsWith("1.8"));

        System.out.println("*** Testing: Simple Https");

        // Reset the ssl stores
        HTTPSession.clearkeystore();

        for(String url : sshurls) {
            System.out.println("*** URL: " + url);
            try (HTTPSession session = HTTPFactory.newSession(url)) {
                this.result = invoke(session, url);
report(this.result);
                Assert.assertTrue("Incorrect return code: " + this.result.status, check(this.result.status));
            }
        }
    }

    @Test
    public void
    testMutualSSH() throws Exception
    {
        String version = System.getProperty("java.version");
        Assume.assumeTrue("Version must be 1.8 (temporary), not: " + version,
                version.startsWith("1.8"));

        System.out.println("*** Testing: Mutual Https");

        // (Re-)Establish the client key store and password
        // Reset the ssl stores
        HTTPSession.rebuildkeystore(Dkeystore, Dkeystorepassword);

        for(String url : sshurls) {
            System.out.println("*** URL: " + url);
            try (HTTPSession session = HTTPFactory.newSession(url)) {
                this.result = invoke(session, url);
                report(this.result);
                Assert.assertTrue("Incorrect return code: " + this.result.status, check(this.result.status));
            }
        }
    }

    //////////////////////////////////////////////////

    protected Result
    invoke(HTTPSession session, String url)
            throws IOException
    {
        Result result = new Result();
        try {
            try (HTTPMethod method = HTTPFactory.Get(session, url)) {
                result.status = method.execute();
                System.err.printf("\tglobal provider: status code = %d\n", result.status);
                //System.err.printf("\t|cache| = %d\n", HTTPCachingProvider.getCache().size());
                // Get the number of calls to the credentialer
                // try to read in the content
                result.contents = readbinaryfile(method.getResponseAsStream());
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException(t);
        }
        return result;
    }
}

/*
javax.net.debug=cmd

where cmd can be:

all            turn on all debugging
ssl            turn on ssl debugging

or

javax.net.debug=ssl:<option>:<option>...

where <option> is taken from the following:

The following can be used with ssl:
	record       enable per-record tracing
	handshake    print each handshake message
	keygen       print key generation data
	session      print session activity
	defaultctx   print default SSL initialization
	sslctx       print SSLContext tracing
	sessioncache print session cache tracing
	keymanager   print key manager tracing
	trustmanager print trust manager tracing
	pluggability print pluggability tracing

	handshake debugging can be widened with:
	data         hex dump of each handshake message
	verbose      verbose handshake message printing

	record debugging can be widened with:
	plaintext    hex dump of record plaintext
	packet       print raw SSL/TLS packets


Process finished with exit code 0
Empty test suite.
*/