/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.ws.repository.test;

import static com.ibm.ws.lars.testutils.BasicChecks.checkCopyFields;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.resources.ToolResource;
import com.ibm.ws.repository.resources.internal.ToolResourceImpl;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

@RunWith(Parameterized.class)
public class ToolResourceTest {

    private final RepositoryConnection repoConnection;

    @Rule
    public final RepositoryFixture fixture;

    @Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return FatUtils.getRestFixtureParameters();
    }

    public ToolResourceTest(RepositoryFixture fixture) {
        this.fixture = fixture;
        this.repoConnection = fixture.getAdminConnection();
    }

    @Test
    public void testIsDownloadable() throws IOException {
        ToolResource tool = WritableResourceFactory.createTool(repoConnection);
        assertEquals("Products should be downloadable",
                     DownloadPolicy.ALL, tool.getDownloadPolicy());
    }

    @Test
    public void testCopyFields() throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoSuchMethodException,
                    SecurityException, InvocationTargetException, IOException {
        checkCopyFields(new ToolResourceImpl(repoConnection), new ToolResourceImpl(repoConnection));
    }
}
