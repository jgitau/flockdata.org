package com.auditbucket.test.functional;

import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.helper.JsonUtils;
import com.auditbucket.registration.bean.SystemUserResultBean;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 28/08/14
 * Time: 2:23 PM
 */
@WebAppConfiguration
public class TestProfileRegistration extends EngineBase {

    @Autowired
    protected WebApplicationContext wac;

    MockMvc mockMvc;


    @Test
    public void testWebRegistrationFlow() throws Exception {
        String companyName = "Public Company";
        setSecurityEmpty();
        // Unauthenticated users can't register accounts
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mockMvc.perform(MockMvcRequestBuilders.post("/profiles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(new RegistrationBean(companyName, sally_admin)))
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // We're now authenticating
        setSecurity();  // An admin user

        // Retry the operation
        SystemUserResultBean regResult = registerSystemUser(new RegistrationBean(companyName, harry));
        assertNotNull(regResult);
        assertEquals(harry, regResult.getLogin());
        assertEquals(harry, regResult.getLogin());
        Assert.assertNotNull(regResult.getApiKey());
        setSecurityEmpty();

        // Check we get back a Guest
        regResult = getMe();
        Assert.assertNotNull(regResult);
        assertEquals("Guest", regResult.getName());
        assertEquals("guest", regResult.getLogin());

        setSecurity(harry);
        regResult = getMe();
        Assert.assertNotNull(regResult);
        assertEquals(harry, regResult.getLogin());
        Assert.assertNotNull(regResult.getApiKey());

        // Assert that harry, who is not an admin, cannot create another user
        mockMvc.perform(MockMvcRequestBuilders.post("/profiles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(new RegistrationBean(companyName, harry)))
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized());


    }

    @Override
    public void cleanUpGraph() {
        super.cleanUpGraph();
    }

    SystemUserResultBean registerSystemUser(RegistrationBean register) throws Exception {

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/profiles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(register))
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    }

    SystemUserResultBean getMe() throws Exception {

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.get("/profiles/me/")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), SystemUserResultBean.class);
    }

}