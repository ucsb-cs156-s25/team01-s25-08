package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;
import edu.ucsb.cs156.example.errors.EntityNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.time.LocalDateTime;

@Tag(name = "RecommendationRequests")
@RestController
@RequestMapping("/api/recommendationrequests")
@Slf4j
public class RecommendationRequestsController extends ApiController {

    @Autowired
    RecommendationRequestRepository recommendationRequestRepository;

    @Operation(summary = "List all recommendation requests")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/all")
    public Iterable<RecommendationRequest> allRecommendationRequests() {
        log.info("GET /api/recommendationrequests/all");
        return recommendationRequestRepository.findAll();
    }

    @Operation(summary = "Create a new recommendation request")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/post")
    public RecommendationRequest postRecommendationRequest(
        @Parameter(name = "requesterEmail") @RequestParam String requesterEmail,
        @Parameter(name = "professorEmail")  @RequestParam String professorEmail,
        @Parameter(name = "explanation")     @RequestParam String explanation,
        @Parameter(name = "dateRequested", description = "ISO date-time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dateRequested,
        @Parameter(name = "dateNeeded",    description = "ISO date-time") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dateNeeded,
        @Parameter(name = "done")         @RequestParam boolean done
    ) {
        log.info("POST /api/recommendationrequests/post");
        RecommendationRequest req = RecommendationRequest.builder()
            .requesterEmail(requesterEmail)
            .professorEmail(professorEmail)
            .explanation(explanation)
            .dateRequested(dateRequested)
            .dateNeeded(dateNeeded)
            .done(done)
            .build();

        return recommendationRequestRepository.save(req);
    }

    /**
     * Get a single recommendationrequest by id
     * 
     * @param id the id of the recommendationrequest
     * @return a RecommendationRequest
     */
    @Operation(summary= "Get a single RecommendationRequest")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("")
    public RecommendationRequest getById(
            @Parameter(name="id") @RequestParam Long id) {
        RecommendationRequest recommendationrequest  = recommendationRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RecommendationRequest.class, id));

        return recommendationrequest;
    }

    /**
     * Update a single recommendation request
     *
     * @param id       id of the recommendation request to update
     * @param incoming the new recommendation request payload
     * @return the updated RecommendationRequest
     */
    @Operation(summary = "Update a single recommendation request")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("")
    public RecommendationRequest updateRecommendationRequest(
            @Parameter(name="id") @RequestParam Long id,
            @RequestBody @Valid RecommendationRequest incoming) {

        RecommendationRequest req = recommendationRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RecommendationRequest.class, id));

        req.setRequesterEmail(incoming.getRequesterEmail());
        req.setProfessorEmail(incoming.getProfessorEmail());
        req.setExplanation(incoming.getExplanation());
        req.setDateRequested(incoming.getDateRequested());
        req.setDateNeeded(incoming.getDateNeeded());
        req.setDone(incoming.getDone());

        return recommendationRequestRepository.save(req);
    }

    /**
     * Delete a RecommendationRequest
     *
     * @param id the id of the recommendation request to delete
     * @return a message indicating the request was deleted
     */
    @Operation(summary = "Delete a recommendation request")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("")
    public Object deleteRecommendationRequest(
            @Parameter(name="id") @RequestParam Long id) {

        RecommendationRequest req = recommendationRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RecommendationRequest.class, id));

        recommendationRequestRepository.delete(req);
        return genericMessage("RecommendationRequest with id %s deleted".formatted(id));
    }
}
