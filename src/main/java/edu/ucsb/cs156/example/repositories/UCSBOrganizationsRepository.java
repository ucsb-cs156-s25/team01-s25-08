package edu.ucsb.cs156.example.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import edu.ucsb.cs156.example.entities.UCSBOrganizations;

@Repository
public interface UCSBOrganizationsRepository extends CrudRepository<UCSBOrganizations, String> {
    // You can add custom query methods here if needed
}