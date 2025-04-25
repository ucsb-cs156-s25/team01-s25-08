package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.Articles;
import edu.ucsb.cs156.example.repositories.ArticlesRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = ArticlesController.class)
@Import(TestConfig.class)
public class ArticlesControllerTests extends ControllerTestCase {

    @MockBean
    ArticlesRepository articlesRepository;

    @MockBean
    UserRepository userRepository;

    // Authorization tests for /api/articles/all

    @Test
    public void logged_out_users_cannot_get_all() throws Exception {
        mockMvc.perform(get("/api/articles/all"))
                .andExpect(status().is(403)); // logged out users can't get all
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_can_get_all() throws Exception {
        mockMvc.perform(get("/api/articles/all"))
                .andExpect(status().is(200)); // logged in users can get all
    }

    @Test
    public void logged_out_users_cannot_get_by_id() throws Exception {
        mockMvc.perform(get("/api/articles?id=7"))
                .andExpect(status().is(403)); // logged out users can't get by id
    }

    // Authorization tests for /api/articles/post
    
    @Test
    public void logged_out_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/articles/post"))
                .andExpect(status().is(403));
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_regular_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/articles/post"))
                .andExpect(status().is(403)); // only admins can post
    }

    // Tests for getById endpoint

    @WithMockUser(roles = { "USER" })
    @Test
    public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {
        // arrange
        LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

        Articles article = Articles.builder()
                .title("Using testing-playground with React Testing Library")
                .url("https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
                .explanation("Helpful article about testing")
                .email("phtcon@ucsb.edu")
                .dateAdded(ldt)
                .build();

        when(articlesRepository.findById(eq(7L))).thenReturn(Optional.of(article));

        // act
        MvcResult response = mockMvc.perform(get("/api/articles?id=7"))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(articlesRepository, times(1)).findById(eq(7L));
        String expectedJson = mapper.writeValueAsString(article);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {
        // arrange
        when(articlesRepository.findById(eq(7L))).thenReturn(Optional.empty());

        // act
        MvcResult response = mockMvc.perform(get("/api/articles?id=7"))
                .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(articlesRepository, times(1)).findById(eq(7L));
        Map<String, Object> json = responseToJson(response);
        assertEquals("EntityNotFoundException", json.get("type"));
        assertEquals("Articles with id 7 not found", json.get("message"));
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_user_can_get_all_articles() throws Exception {

        // arrange
        LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

        Articles article1 = Articles.builder()
                .title("Using testing-playground with React Testing Library")
                .url("https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
                .explanation("Helpful article about testing")
                .email("phtcon@ucsb.edu")
                .dateAdded(ldt1)
                .build();

        LocalDateTime ldt2 = LocalDateTime.parse("2022-04-09T00:00:00");

        Articles article2 = Articles.builder()
                .title("Handy Spring Utility Classes")
                .url("https://twitter.com/maciejwalkowiak/status/1511736828369719300")
                .explanation("A lot of really useful classes are built into Spring")
                .email("phtcon@ucsb.edu")
                .dateAdded(ldt2)
                .build();

        ArrayList<Articles> expectedArticles = new ArrayList<>();
        expectedArticles.addAll(Arrays.asList(article1, article2));

        when(articlesRepository.findAll()).thenReturn(expectedArticles);

        // act
        MvcResult response = mockMvc.perform(get("/api/articles/all"))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(articlesRepository, times(1)).findAll();
        String expectedJson = mapper.writeValueAsString(expectedArticles);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void an_admin_user_can_post_a_new_article() throws Exception {
        // arrange
        LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

        Articles articleToPost = Articles.builder()
                .title("Using testing-playground with React Testing Library")
                .url("https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
                .explanation("Helpful article about testing")
                .email("phtcon@ucsb.edu")
                .dateAdded(ldt)
                .build();

        Articles articleToReturn = Articles.builder()
                .id(42L)
                .title("Using testing-playground with React Testing Library")
                .url("https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
                .explanation("Helpful article about testing")
                .email("phtcon@ucsb.edu")
                .dateAdded(ldt)
                .build();

        when(articlesRepository.save(any(Articles.class))).thenReturn(articleToReturn);

        // act
        MvcResult response = mockMvc.perform(
                post("/api/articles/post?title=Using testing-playground with React Testing Library&url=https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7&explanation=Helpful article about testing&email=phtcon@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(articlesRepository, times(1)).save(any(Articles.class));
        String expectedJson = mapper.writeValueAsString(articleToReturn);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_can_post_article_with_special_characters() throws Exception {
        // arrange
        LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

        Articles articleToReturn = Articles.builder()
                .id(42L)
                .title("Article with special chars: &<>\"'")
                .url("https://example.org/article?param=value&other=123")
                .explanation("Testing & special < > characters")
                .email("test@ucsb.edu")
                .dateAdded(ldt)
                .build();

        when(articlesRepository.save(any(Articles.class))).thenReturn(articleToReturn);

        // act
        MvcResult response = mockMvc.perform(
                post("/api/articles/post?title=Article with special chars: &<>\"'&url=https://example.org/article?param=value&other=123&explanation=Testing & special < > characters&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(articlesRepository, times(1)).save(any(Articles.class));
        String expectedJson = mapper.writeValueAsString(articleToReturn);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_can_post_article_with_empty_explanation() throws Exception {
        // arrange
        LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

        Articles articleToReturn = Articles.builder()
                .id(42L)
                .title("Article with empty explanation")
                .url("https://example.org/article")
                .explanation("")
                .email("test@ucsb.edu")
                .dateAdded(ldt)
                .build();

        when(articlesRepository.save(any(Articles.class))).thenReturn(articleToReturn);

        // act
        MvcResult response = mockMvc.perform(
                post("/api/articles/post?title=Article with empty explanation&url=https://example.org/article&explanation=&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(articlesRepository, times(1)).save(any(Articles.class));
        String expectedJson = mapper.writeValueAsString(articleToReturn);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_post_article_saves_dateAdded_correctly() throws Exception {
        // arrange
        Articles savedArticle = Articles.builder()
                .id(42L)
                .title("Test Article")
                .url("https://example.org")
                .explanation("Test explanation")
                .email("test@ucsb.edu")
                .dateAdded(LocalDateTime.now())
                .build();

        // mockito argument captor to capture the Article passed to save
        AtomicReference<Articles> capturedArticle = new AtomicReference<>();
        when(articlesRepository.save(any(Articles.class))).thenAnswer(invocation -> {
            Articles articleToSave = invocation.getArgument(0);
            capturedArticle.set(articleToSave);
            articleToSave.setId(42L); // simulate DB auto-generating id
            return articleToSave;
        });

        // act
        MvcResult response = mockMvc.perform(
                post("/api/articles/post?title=Test Article&url=https://example.org&explanation=Test explanation&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        String responseString = response.getResponse().getContentAsString();
        Articles savedArticleFromResponse = mapper.readValue(responseString, Articles.class);
        
        // Verify the date is within the last second (since we can't know the exact time)
        LocalDateTime savedAt = capturedArticle.get().getDateAdded();
        LocalDateTime now = LocalDateTime.now();
        assertTrue(savedAt.isAfter(now.minusSeconds(1)));
        assertTrue(savedAt.isBefore(now.plusSeconds(1)));

        // Verify all other fields were set correctly
        assertEquals("Test Article", capturedArticle.get().getTitle());
        assertEquals("https://example.org", capturedArticle.get().getUrl());
        assertEquals("Test explanation", capturedArticle.get().getExplanation());
        assertEquals("test@ucsb.edu", capturedArticle.get().getEmail());
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_post_article_with_long_strings() throws Exception {
        // arrange
        String longTitle = "A".repeat(255);
        String longUrl = "https://example.org/" + "A".repeat(200);
        String longExplanation = "B".repeat(255);
        String longEmail = "a".repeat(200) + "@ucsb.edu";

        Articles expectedArticle = Articles.builder()
                .id(42L)
                .title(longTitle)
                .url(longUrl)
                .explanation(longExplanation)
                .email(longEmail)
                .dateAdded(LocalDateTime.now())
                .build();

        when(articlesRepository.save(any(Articles.class))).thenReturn(expectedArticle);

        // act
        MvcResult response = mockMvc.perform(
                post("/api/articles/post?title=" + longTitle + "&url=" + longUrl + "&explanation=" + longExplanation + "&email=" + longEmail)
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(articlesRepository, times(1)).save(any(Articles.class));
        String responseString = response.getResponse().getContentAsString();
        Articles savedArticle = mapper.readValue(responseString, Articles.class);
        
        assertEquals(longTitle, savedArticle.getTitle());
        assertEquals(longUrl, savedArticle.getUrl());
        assertEquals(longExplanation, savedArticle.getExplanation());
        assertEquals(longEmail, savedArticle.getEmail());
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_post_article_with_null_fields() throws Exception {
        // arrange
        Articles expectedArticle = Articles.builder()
                .id(42L)
                .title("Test Title")
                .url("https://example.org")
                .explanation(null)
                .email("test@ucsb.edu")
                .dateAdded(LocalDateTime.now())
                .build();

        when(articlesRepository.save(any(Articles.class))).thenReturn(expectedArticle);

        // act
        MvcResult response = mockMvc.perform(
                post("/api/articles/post?title=Test Title&url=https://example.org&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(articlesRepository, times(1)).save(any(Articles.class));
        String responseString = response.getResponse().getContentAsString();
        Articles savedArticle = mapper.readValue(responseString, Articles.class);
        
        assertNull(savedArticle.getExplanation());
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_post_article_check_all_fields_in_saved_article() throws Exception {
        // arrange
        AtomicReference<Articles> capturedArticle = new AtomicReference<>();
        when(articlesRepository.save(any(Articles.class))).thenAnswer(invocation -> {
            Articles articleToSave = invocation.getArgument(0);
            capturedArticle.set(articleToSave);
            articleToSave.setId(42L);
            return articleToSave;
        });

        // act
        mockMvc.perform(
                post("/api/articles/post?title=Test Title&url=https://example.org&explanation=Test explanation&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk());

        // assert
        Articles savedArticle = capturedArticle.get();
        assertNotNull(savedArticle.getDateAdded(), "dateAdded should not be null");
        assertEquals("Test Title", savedArticle.getTitle());
        assertEquals("https://example.org", savedArticle.getUrl());
        assertEquals("Test explanation", savedArticle.getExplanation());
        assertEquals("test@ucsb.edu", savedArticle.getEmail());
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_post_article_test_required_fields() throws Exception {
        // Test missing title
        mockMvc.perform(
                post("/api/articles/post?url=https://example.org&explanation=test&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().is4xxClientError());

        // Test missing url
        mockMvc.perform(
                post("/api/articles/post?title=Test&explanation=test&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().is4xxClientError());

        // Test missing email
        mockMvc.perform(
                post("/api/articles/post?title=Test&url=https://example.org&explanation=test")
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_post_article_test_empty_required_fields() throws Exception {
        // Test empty title
        mockMvc.perform(
                post("/api/articles/post?title=&url=https://example.org&explanation=test&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk());

        // Test empty url
        mockMvc.perform(
                post("/api/articles/post?title=Test&url=&explanation=test&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk());

        // Test empty email
        mockMvc.perform(
                post("/api/articles/post?title=Test&url=https://example.org&explanation=test&email=")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_post_article_test_date_added() throws Exception {
        // arrange
        LocalDateTime beforeTime = LocalDateTime.now();

        Articles expectedArticle = Articles.builder()
                .id(42L)
                .title("Test")
                .url("https://example.org")
                .explanation("test")
                .email("test@ucsb.edu")
                .dateAdded(beforeTime)
                .build();

        when(articlesRepository.save(any(Articles.class))).thenReturn(expectedArticle);

        // act
        MvcResult response = mockMvc.perform(
                post("/api/articles/post?title=Test&url=https://example.org&explanation=test&email=test@ucsb.edu")
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        String responseString = response.getResponse().getContentAsString();
        Articles savedArticle = mapper.readValue(responseString, Articles.class);
        
        LocalDateTime afterTime = LocalDateTime.now();
        LocalDateTime savedAt = savedArticle.getDateAdded();
        
        assertTrue(savedAt.isAfter(beforeTime.minusSeconds(1)));
        assertTrue(savedAt.isBefore(afterTime.plusSeconds(1)));
    }

    @Test
    public void logged_out_users_cannot_put() throws Exception {
        mockMvc.perform(put("/api/articles?id=1"))
                .andExpect(status().is(403));
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_regular_users_cannot_put() throws Exception {
        mockMvc.perform(put("/api/articles?id=1"))
                .andExpect(status().is(403)); // only admins can put
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_can_edit_an_existing_article() throws Exception {
        // arrange
        LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
        LocalDateTime ldt2 = LocalDateTime.parse("2023-01-03T00:00:00");

        Articles articleOrig = Articles.builder()
                .title("Using testing-playground with React Testing Library")
                .url("https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
                .explanation("Helpful article about testing")
                .email("phtcon@ucsb.edu")
                .dateAdded(ldt1)
                .build();

        Articles articleEdited = Articles.builder()
                .title("Updated Title")
                .url("https://example.org/updated")
                .explanation("Updated explanation")
                .email("updated@ucsb.edu")
                .dateAdded(ldt2)
                .build();

        String requestBody = mapper.writeValueAsString(articleEdited);

        when(articlesRepository.findById(eq(67L))).thenReturn(Optional.of(articleOrig));

        // act
        MvcResult response = mockMvc.perform(
                put("/api/articles?id=67")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(articlesRepository, times(1)).findById(67L);
        verify(articlesRepository, times(1)).save(articleEdited);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(requestBody, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_cannot_edit_article_that_does_not_exist() throws Exception {
        // arrange
        LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

        Articles articleEdited = Articles.builder()
                .title("Updated Title")
                .url("https://example.org/updated")
                .explanation("Updated explanation")
                .email("updated@ucsb.edu")
                .dateAdded(ldt1)
                .build();

        String requestBody = mapper.writeValueAsString(articleEdited);

        when(articlesRepository.findById(eq(67L))).thenReturn(Optional.empty());

        // act
        MvcResult response = mockMvc.perform(
                put("/api/articles?id=67")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(articlesRepository, times(1)).findById(67L);
        Map<String, Object> json = responseToJson(response);
        assertEquals("Articles with id 67 not found", json.get("message"));
    }
}
