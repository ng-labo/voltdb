/* This file is part of VoltDB.
 * Copyright (C) 2008-2012 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.voltdb;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ProcCallException;
import org.voltdb.compiler.VoltProjectBuilder;

public class TestDefaultDeployment extends TestCase {

    public void testDefaultDeploymentInitialization() throws InterruptedException, IOException, ProcCallException {
        String ddl =
            "CREATE TABLE WAREHOUSE (" +
            "W_ID INTEGER DEFAULT '0' NOT NULL, "+
            "W_NAME VARCHAR(16) DEFAULT NULL, " +
            "PRIMARY KEY  (W_ID)" +
            ");";

        VoltProjectBuilder builder = new VoltProjectBuilder();
        builder.addLiteralSchema(ddl);
        builder.addProcedures(
                org.voltdb.compiler.procedures.MilestoneOneInsert.class,
                org.voltdb.compiler.procedures.MilestoneOneSelect.class,
                org.voltdb.compiler.procedures.MilestoneOneCombined.class);

        // compileWithDefaultDeployment() generates no deployment.xml so that the default is used.
        assertTrue(builder.compileWithDefaultDeployment("test.jar"));

        final File jar = new File("test.jar");
        jar.deleteOnExit();

        String pathToDeployment = builder.getPathToDeployment();
        assertEquals(pathToDeployment, null);

        // start VoltDB server using hsqlsb backend
        ServerThread server = new ServerThread("test.jar", null, BackendTarget.HSQLDB_BACKEND);
        server.start();
        server.waitForInitialization();

        // run the test
        ClientConfig config = new ClientConfig("program", "none");
        Client client = ClientFactory.createClient(config);
        client.createConnection("localhost");

        // call the insert procedure
        VoltTable[] results = client.callProcedure("MilestoneOneCombined", 99L, "TEST").getResults();
        // check one table was returned
        assertTrue(results.length == 1);
        // check one tuple was modified
        VoltTable result = results[0];
        VoltTableRow row = result.fetchRow(0);
        String resultStr = row.getString(0);
        assertTrue(resultStr.equals("TEST"));

        server.shutdown();
        server.join();
    }

}
