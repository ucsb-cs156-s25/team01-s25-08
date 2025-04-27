package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = RecommendationRequestsController.class)
@Import(TestConfig.class)
public class RecommendationRequestsControllerTests extends ControllerTestCase {

    @MockBean
    RecommendationRequestRepository recommendationRequestRepository;

    @MockBean
    UserRepository userRepository;

    @Test
    public void logged_out_users_cannot_get_all() throws Exception {
        mockMvc.perform(get("/api/recommendationrequests/all"))
            .andExpect(status().is(403)); // logged out users can't get all
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_can_get_all() throws Exception {
        mockMvc.perform(get("/api/recommendationrequests/all"))
            .andExpect(status().is(200)); // logged in users can get all
    }

    @Test
    public void logged_out_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/recommendationrequests/post"))
            .andExpect(status().is(403));
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_regular_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/recommendationrequests/post"))
            .andExpect(status().is(403)); // only admins can post
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_user_can_get_all_recommendationrequests() throws Exception {

        // arrange
        LocalDateTime dateRequested1 = LocalDateTime.parse("2022-04-20T00:00:00");
        LocalDateTime dateNeeded1 = LocalDateTime.parse("2022-05-01T00:00:00");

        RecommendationRequest request1 = RecommendationRequest.builder()
            .requesterEmail("cgaucho@ucsb.edu")
            .professorEmail("phtcon@ucsb.edu")
            .explanation("BS/MS program")
            .dateRequested(dateRequested1)
            .dateNeeded(dateNeeded1)
            .done(false)
            .build();

        LocalDateTime dateRequested2 = LocalDateTime.parse("2022-05-20T00:00:00");
        LocalDateTime dateNeeded2 = LocalDateTime.parse("2022-11-15T00:00:00");

        RecommendationRequest request2 = RecommendationRequest.builder()
            .requesterEmail("ldelplaya@ucsb.edu")
            .professorEmail("richert@ucsb.edu")
            .explanation("PhD CS Stanford")
            .dateRequested(dateRequested2)
            .dateNeeded(dateNeeded2)
            .done(false)
            .build();

        ArrayList<RecommendationRequest> expectedRequests = new ArrayList<>();
        expectedRequests.addAll(Arrays.asList(request1, request2));

        when(recommendationRequestRepository.findAll()).thenReturn(expectedRequests);

        // act
        MvcResult response = mockMvc.perform(get("/api/recommendationrequests/all"))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(recommendationRequestRepository, times(1)).findAll();
        String expectedJson = mapper.writeValueAsString(expectedRequests);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void an_admin_user_can_post_a_new_recommendationrequest() throws Exception {
        // arrange
        LocalDateTime dateRequested = LocalDateTime.parse("2022-04-20T00:00:00");
        LocalDateTime dateNeeded = LocalDateTime.parse("2022-05-01T00:00:00");

        RecommendationRequest request = RecommendationRequest.builder()
            .requesterEmail("cgaucho@ucsb.edu")
            .professorEmail("phtcon@ucsb.edu")
            .explanation("BS/MS program")
            .dateRequested(dateRequested)
            .dateNeeded(dateNeeded)
            .done(false)
            .build();

        when(recommendationRequestRepository.save(eq(request))).thenReturn(request);

        // act
        MvcResult response = mockMvc.perform(
                post("/api/recommendationrequests/post?requesterEmail=cgaucho@ucsb.edu&professorEmail=phtcon@ucsb.edu&explanation=BS/MS program&dateRequested=2022-04-20T00:00:00&dateNeeded=2022-05-01T00:00:00&done=false")
                    .with(csrf()))
            .andExpect(status().isOk()).andReturn();

        // assert
        verify(recommendationRequestRepository, times(1)).save(request);
        String expectedJson = mapper.writeValueAsString(request);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @Test
    public void logged_out_users_cannot_get_by_id() throws Exception {
        mockMvc.perform(get("/api/recommendationrequests?id=7"))
               .andExpect(status().is(403)); // logged out users can't get by id
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {
        // arrange
        when(recommendationRequestRepository.findById(eq(7L)))
            .thenReturn(Optional.empty());

        // act
        MvcResult response = mockMvc.perform(get("/api/recommendationrequests?id=7"))
                                    .andExpect(status().isNotFound())
                                    .andReturn();

        // assert
        verify(recommendationRequestRepository, times(1)).findById(eq(7L));
        Map<String, Object> json = responseToJson(response);
        assertEquals("EntityNotFoundException", json.get("type"));
        assertEquals("RecommendationRequest with id 7 not found", json.get("message"));
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void test_that_logged_in_user_can_get_by_id_when_the_id_does_exist() throws Exception {
        // arrange

        LocalDateTime dateRequested = LocalDateTime.parse("2022-04-20T00:00:00");
        LocalDateTime dateNeeded = LocalDateTime.parse("2022-05-01T00:00:00");

        RecommendationRequest request = RecommendationRequest.builder()
            .requesterEmail("cgaucho@ucsb.edu")
            .professorEmail("phtcon@ucsb.edu")
            .explanation("BS/MS program")
            .dateRequested(dateRequested)
            .dateNeeded(dateNeeded)
            .done(false)
            .build();

        when(recommendationRequestRepository.findById(eq(7L)))
            .thenReturn(Optional.of(request));

        // act
        MvcResult response = mockMvc.perform(get("/api/recommendationrequests?id=7"))
                                    .andExpect(status().isOk())
                                    .andReturn();

        // assert
        verify(recommendationRequestRepository, times(1)).findById(eq(7L));
        String expectedJson = mapper.writeValueAsString(request);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }


    // new tests for put method on RecommendationRequest

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_can_edit_an_existing_recommendationrequest() throws Exception {
        // arrange
        LocalDateTime dateRequestedOrig = LocalDateTime.parse("2022-01-03T00:00:00");
        LocalDateTime dateNeededOrig    = LocalDateTime.parse("2022-02-03T00:00:00");

        RecommendationRequest orig = RecommendationRequest.builder()
            .requesterEmail("alice@example.com")
            .professorEmail("prof@example.edu")
            .explanation("Original explanation")
            .dateRequested(dateRequestedOrig)
            .dateNeeded(dateNeededOrig)
            .done(false)
            .build();

        LocalDateTime dateRequestedEdit = LocalDateTime.parse("2023-01-03T00:00:00");
        LocalDateTime dateNeededEdit    = LocalDateTime.parse("2023-02-03T00:00:00");

        RecommendationRequest edited = RecommendationRequest.builder()
            .requesterEmail("bob@example.com")
            .professorEmail("advisor@example.edu")
            .explanation("Edited explanation")
            .dateRequested(dateRequestedEdit)
            .dateNeeded(dateNeededEdit)
            .done(true)
            .build();

        String requestBody = mapper.writeValueAsString(edited);

        // stub out findById and save
        when(recommendationRequestRepository.findById(eq(67L)))
            .thenReturn(Optional.of(orig));
        when(recommendationRequestRepository.save(eq(edited)))
            .thenReturn(edited);

        // act
        MvcResult response = mockMvc.perform(
                put("/api/recommendationrequests?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

        // assert
        verify(recommendationRequestRepository, times(1)).findById(67L);
        verify(recommendationRequestRepository, times(1)).save(edited);

        String responseString = response.getResponse().getContentAsString();
        assertEquals(requestBody, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_cannot_edit_recommendationrequest_that_does_not_exist() throws Exception {
        // arrange
        LocalDateTime dateRequested = LocalDateTime.parse("2022-01-03T00:00:00");
        LocalDateTime dateNeeded    = LocalDateTime.parse("2022-02-03T00:00:00");

        RecommendationRequest edited = RecommendationRequest.builder()
            .requesterEmail("alice@example.com")
            .professorEmail("prof@example.edu")
            .explanation("Will not be saved")
            .dateRequested(dateRequested)
            .dateNeeded(dateNeeded)
            .done(false)
            .build();

        String requestBody = mapper.writeValueAsString(edited);

        when(recommendationRequestRepository.findById(eq(67L)))
            .thenReturn(Optional.empty());

        // act
        MvcResult response = mockMvc.perform(
                put("/api/recommendationrequests?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

        // assert
        verify(recommendationRequestRepository, times(1)).findById(67L);
        Map<String, Object> json = responseToJson(response);
        assertEquals("RecommendationRequest with id 67 not found", json.get("message"));
    }
}
